package org.jeecg.common.system.query;

import com.baomidou.mybatisplus.core.conditions.SharedString;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.enums.SqlKeyword;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * <p>Created on 2021/7/29.</p>
 * <p>
 * 必须要保证程序初始化之后才能使用该类，因为要解析出TableInfo
 *
 * @author Zoe
 */
public class LtmaiQueryWrapper<T> extends QueryWrapper<T> {

    Map<String, LtmaiQueryWrapper<?>> subSqlMap  = new ConcurrentHashMap<>();
    Map<String, Boolean>              subSqlFlag = new ConcurrentHashMap<>();

    TableInfo tableInfo;

    public LtmaiQueryWrapper(T entity) {
        super(entity);
        this.tableInfo = TableInfoHelper.getTableInfo(entity.getClass());
    }

    protected LtmaiQueryWrapper(String select, Class<T> entityClass, AtomicInteger paramNameSeq, Map<String, Object> paramNameValuePairs) {
        super.setEntityClass(entityClass);
        this.select(select);
        this.paramNameSeq        = paramNameSeq;
        this.paramNameValuePairs = paramNameValuePairs;
        this.expression          = new MergeSegments();
        this.lastSql             = SharedString.emptyString();
        this.sqlComment          = SharedString.emptyString();
        this.sqlFirst            = SharedString.emptyString();
        this.tableInfo           = TableInfoHelper.getTableInfo(entityClass);
    }

    /**
     * 第一步
     *
     * @param relColumn
     * @param subTable
     */
    public void initSubTable(String relColumn, String subTable) {
        if (subSqlMap.get(subTable) != null) {
            return;
        }
        TableInfo subTableInfo = Optional.ofNullable(TableInfoHelper.getTableInfo(subTable)).orElseThrow(() -> new IllegalArgumentException("找不到" + subTable));
        String selectRelColumn = subTableInfo.getFieldList()
                                             .stream()
                                             .filter(ifMatchColumnOrProperty(relColumn))
                                             .findFirst()
                                             .map(TableFieldInfo::getSqlSelect)
                                             .orElseThrow(() -> new IllegalArgumentException("找不到" + subTable + "." + relColumn));
        subSqlMap.putIfAbsent(subTable, new LtmaiQueryWrapper<>(selectRelColumn, subTableInfo.getClass(),
                                                                this.paramNameSeq, this.paramNameValuePairs));
        subSqlFlag.putIfAbsent(subTable, false);
    }

    /**
     * 第二步
     *
     * @param matchType
     * @param subTable
     * @param condColumn
     * @param rule
     * @param value
     */
    public void inSubTable(MatchTypeEnum matchType, String subTable, String condColumn, QueryRuleEnum rule, Object value) {
        LtmaiQueryWrapper<?> subWhere = subSqlMap.get(subTable);
        Boolean              flag     = subSqlFlag.get(subTable);
        if (subWhere == null) {
            return;
        }
        if (subWhere.getExpression().getNormal().isEmpty()) {
            QueryGenerator.addEasyQuery(subWhere, condColumn, rule, value);
        }
        else if (MatchTypeEnum.OR.equals(matchType)) {
            subWhere.or(queryWrapper -> QueryGenerator.addEasyQuery(queryWrapper, condColumn, rule, value));
        }
        else {
            subWhere.and(queryWrapper -> QueryGenerator.addEasyQuery(queryWrapper, condColumn, rule, value));
        }
        if (flag == null || !flag) {
            this.doIt(true, () -> "id", SqlKeyword.IN,
                      () -> String.format("(SELECT %s FROM %s WHERE %s)", subWhere.getSqlSelect(), subTable, subWhere.getSqlSegment()));
            subSqlFlag.put(subTable, true);
        }
    }

    /**
     * 兼容原生sql字段或java类成员变量名
     *
     * @param subSelect
     * @return
     */
    private Predicate<TableFieldInfo> ifMatchColumnOrProperty(String subSelect) {
        return tableFieldInfo -> tableFieldInfo.getColumn().equals(subSelect) || tableFieldInfo.getProperty().equals(subSelect);
    }
}
