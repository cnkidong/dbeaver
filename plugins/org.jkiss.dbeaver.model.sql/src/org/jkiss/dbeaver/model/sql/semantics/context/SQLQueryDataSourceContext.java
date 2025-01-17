/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.sql.semantics.context;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLSearchUtils;
import org.jkiss.dbeaver.model.sql.parser.SQLIdentifierDetector;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsSourceModel;
import org.jkiss.dbeaver.model.sql.semantics.model.select.SQLQueryRowsTableValueModel;
import org.jkiss.dbeaver.model.stm.STMTreeNode;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Represents underlying database context having real tables
 */
public class SQLQueryDataSourceContext extends SQLQueryDataContext {
    @NotNull
    private final SQLDialect dialect;
    @NotNull
    private final DBCExecutionContext executionContext;
    @NotNull
    private final SQLIdentifierDetector identifierDetector;
    @NotNull
    private final Map<String, SQLQueryResultPseudoColumn> globalPseudoColumnsByCanonicalName;
    @NotNull
    private final Function<SQLQueryRowsSourceModel, List<SQLQueryResultPseudoColumn>> rowsetPseudoColumnsProvider;

    public SQLQueryDataSourceContext(
        @NotNull SQLDialect dialect,
        @NotNull DBCExecutionContext executionContext,
        @NotNull Map<String, SQLQueryResultPseudoColumn> globalPseudoColumns,
        @NotNull Function<SQLQueryRowsSourceModel, List<SQLQueryResultPseudoColumn>> rowsetPseudoColumnsProvider
    ) {
        this.dialect = dialect;
        this.executionContext = executionContext;
        this.identifierDetector = new SQLIdentifierDetector(dialect);
        this.globalPseudoColumnsByCanonicalName = globalPseudoColumns;
        this.rowsetPseudoColumnsProvider = rowsetPseudoColumnsProvider;
    }

    @NotNull
    @Override
    public List<SQLQueryResultColumn> getColumnsList() {
        return Collections.emptyList();
    }

    @Override
    public boolean hasUndresolvedSource() {
        return false;
    }


    @NotNull
    @Override
    public List<SQLQueryResultPseudoColumn> getPseudoColumnsList() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public DBSEntity findRealTable(@NotNull DBRProgressMonitor monitor, @NotNull List<String> tableName) {
        if (this.executionContext.getDataSource() instanceof DBSObjectContainer container) {
            List<String> tableName2 = new ArrayList<>(tableName);
            DBSObject obj = SQLSearchUtils.findObjectByFQN(
                monitor,
                container,
                this.executionContext,
                tableName2,
                false,
                identifierDetector
            );
            return obj instanceof DBSTable table ? table : (obj instanceof DBSView view ? view : null);
        } else {
            // Semantic analyser should never be used for databases, which doesn't support table lookup
            // It's managed by LSMDialectRegistry (see org.jkiss.dbeaver.lsm.dialectSyntax extension point)
            // so that analyzers could be created only for supported dialects.
            throw new UnsupportedOperationException("Semantic analyser should never be used for databases, which doesn't support table lookup");
        }
    }

    @Nullable
    @Override
    public DBSObject findRealObject(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObjectType objectType,
        @NotNull List<String> objectName
    ) {
        if (this.executionContext.getDataSource() instanceof DBSObjectContainer container) {
            List<String> objectName2 = new ArrayList<>(objectName);
            DBSObject obj = SQLSearchUtils.findObjectByFQN(
                monitor,
                container,
                this.executionContext,
                objectName2,
                false,
                identifierDetector
            );
            return objectType.getTypeClass().isInstance(obj) ? obj : null;
        } else {
            // Semantic analyser should never be used for databases, which doesn't support table lookup
            // It's managed by LSMDialectRegistry (see org.jkiss.dbeaver.lsm.dialectSyntax extension point)
            // so that analyzers could be created only for supported dialects.
            throw new UnsupportedOperationException("Semantic analyser should never be used for databases, which doesn't support table lookup");
        }
    }

    @Nullable
    @Override
    public SQLQueryRowsSourceModel findRealSource(@NotNull DBSEntity table) {
        return null;
    }

    @Nullable
    @Override
    public SQLQueryResultColumn resolveColumn(@NotNull DBRProgressMonitor monitor, @NotNull String simpleName) {
        return null;
    }

    @Nullable
    @Override
    public SQLQueryResultPseudoColumn resolvePseudoColumn(@NotNull DBRProgressMonitor monitor, @NotNull String name) {
        return null;
    }

    @Nullable
    @Override
    public SQLQueryResultPseudoColumn resolveGlobalPseudoColumn(@NotNull DBRProgressMonitor monitor, @NotNull String name) {
        return this.globalPseudoColumnsByCanonicalName.get(name);
    }

    @NotNull
    @Override
    public SQLDialect getDialect() {
        return this.dialect;
    }

    @Override
    protected void collectKnownSourcesImpl(@NotNull KnownSourcesInfo result) {
        // no sources have been referenced yet, so nothing to register
    }

    @Override
    protected List<SQLQueryResultPseudoColumn> prepareRowsetPseudoColumns(@NotNull SQLQueryRowsSourceModel source) {
        return this.rowsetPseudoColumnsProvider.apply(source);
    }
}
