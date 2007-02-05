// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006 Talend - www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.sqlbuilder.repository.utility;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.talend.core.model.metadata.IMetadataConnection;
import org.talend.core.model.metadata.builder.ConvertionHelper;
import org.talend.core.model.metadata.builder.connection.DatabaseConnection;
import org.talend.core.model.metadata.builder.connection.MetadataColumn;
import org.talend.core.model.metadata.builder.connection.MetadataTable;
import org.talend.core.model.metadata.builder.connection.QueriesConnection;
import org.talend.core.model.metadata.builder.connection.Query;
import org.talend.core.model.metadata.builder.database.ExtractMetaDataUtils;
import org.talend.core.model.properties.DatabaseConnectionItem;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.RepositoryNode.EProperties;
import org.talend.sqlbuilder.Messages;
import org.talend.sqlbuilder.SqlBuilderPlugin;
import org.talend.sqlbuilder.dbstructure.RepositoryNodeType;
import org.talend.sqlbuilder.dbstructure.DBTreeProvider.MetadataColumnRepositoryObject;
import org.talend.sqlbuilder.dbstructure.DBTreeProvider.MetadataTableRepositoryObject;

/**
 * qzhang class global comment. Detailled comment <br/>
 * 
 * $Id: talend-code-templates.xml 1 2006-09-29 17:06:40 +0000 (Fri, 29 Sep 2006) nrousseau $
 * 
 */
public class EMFRepositoryNodeManager {

    private DatabaseMetaData dbMetaData;

    private SQLBuilderRepositoryNodeManager rnmanager = new SQLBuilderRepositoryNodeManager();

    /**
     * dev Comment method "getQueryByLabel".
     * 
     * @param node all repository node Type
     * @param label search query label
     * @return
     */
    @SuppressWarnings("unchecked") //$NON-NLS-1$
    public static Query getQueryByLabel(RepositoryNode node, String label) {
        RepositoryNode root = null;
        if (node.getObjectType().equals(ERepositoryObjectType.METADATA_CON_QUERY)) {
            root = node.getParent().getParent();
        } else {
            root = SQLBuilderRepositoryNodeManager.getRoot(node);
        }
        if (root == null) {
            return null;
        }
        DatabaseConnectionItem item = SQLBuilderRepositoryNodeManager.getItem(root);
        DatabaseConnection connection = (DatabaseConnection) item.getConnection();
        QueriesConnection queriesConnection = (QueriesConnection) connection.getQueries().get(0);
        List<Query> queries = queriesConnection.getQuery();
        for (Query query : queries) {
            if (query.getLabel().equals(label)) {
                return query;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked") //$NON-NLS-1$
    public List<MetadataTable> getTables(List<RepositoryNode> nodes, List<MetadataColumn> selectedColumns) {
        List<MetadataTable> tables = new ArrayList<MetadataTable>();
        IMetadataConnection iMetadataConnection = null;
        RepositoryNode root = null;
        for (RepositoryNode node : nodes) {
            RepositoryNodeType type = SQLBuilderRepositoryNodeManager.getRepositoryType(node);
            if (type == RepositoryNodeType.DATABASE) {
                root = node;
                DatabaseConnection connection = (DatabaseConnection) SQLBuilderRepositoryNodeManager.getItem(node)
                        .getConnection();
                for (MetadataTable table : (List<MetadataTable>) connection.getTables()) {
                    if (!tables.contains(table)) {
                        tables.add(table);
                        selectedColumns.addAll(table.getColumns());
                    }
                }
                // if database is selected , It does not need to check others.
                break;
            } else if (type == RepositoryNodeType.TABLE) {
                MetadataTable table = ((MetadataTableRepositoryObject) node.getObject()).getTable();
                if (!tables.contains(table)) {
                    tables.add(table);
                    selectedColumns.addAll(table.getColumns());
                }
                if (root == null) {
                    root = SQLBuilderRepositoryNodeManager.getRoot(node);
                }

            } else if (type == RepositoryNodeType.COLUMN) {

                MetadataColumn column = ((MetadataColumnRepositoryObject) node.getObject()).getColumn();
                if (!selectedColumns.contains(column)) {
                    selectedColumns.add(column);
                }

                MetadataTable table = column.getTable();
                if (!tables.contains(table)) {
                    tables.add(table);
                }
                if (root == null) {
                    root = SQLBuilderRepositoryNodeManager.getRoot(node);
                }
            }
            if (root != null) {
                iMetadataConnection = ConvertionHelper.convert((DatabaseConnection) SQLBuilderRepositoryNodeManager.getItem(root)
                        .getConnection());
                dbMetaData = rnmanager.getDatabaseMetaData(iMetadataConnection);
            }

        }
        return tables;
    }

    public List<String[]> getPKFromTables(List<MetadataTable> tables) {
        List<String[]> fks = new ArrayList<String[]>();
        String fk = ""; //$NON-NLS-1$
        String pk = ""; //$NON-NLS-1$
        for (MetadataTable table : tables) {
            try {
                if (dbMetaData != null) {
                    ResultSet resultSet = dbMetaData.getExportedKeys("", ExtractMetaDataUtils.schema, table.getSourceName()); //$NON-NLS-1$
                    ResultSetMetaData metadata = resultSet.getMetaData();
                    int[] relevantIndeces = new int[metadata.getColumnCount()];
                    for (int i = 1; i <= metadata.getColumnCount(); i++) {
                        relevantIndeces[i - 1] = i;
                    }

                    while (resultSet.next()) {
                        for (int i = 0; i < relevantIndeces.length; i++) {
                            String key = metadata.getColumnName(relevantIndeces[i]);
                            if (key.equals("FKCOLUMN_NAME")) { //$NON-NLS-1$
                                fk += resultSet.getString(relevantIndeces[i]);
                            } else if (key.equals("FKTABLE_NAME")) { //$NON-NLS-1$
                                fk = resultSet.getString(relevantIndeces[i]) + "."; //$NON-NLS-1$
                            } else if (key.equals("PKCOLUMN_NAME")) { //$NON-NLS-1$
                                pk = table.getSourceName() + "." + resultSet.getString(relevantIndeces[i]); //$NON-NLS-1$
                            }
                        }
                        if (!"".equals(fk) && !"".equals(pk)) { //$NON-NLS-1$ //$NON-NLS-2$
                            String[] strs = new String[2];
                            strs[0] = pk;
                            strs[1] = fk;
                            fks.add(strs);
                            fk = ""; //$NON-NLS-1$
                            pk = ""; //$NON-NLS-1$
                        }
                    }
                    resultSet.close();
                }

            } catch (Exception e) {
                SqlBuilderPlugin.log(Messages.getString("EMFRepositoryNodeManager.logMessage"), e); //$NON-NLS-1$
            }
        }
        return fks;
    }

    private static List<RepositoryNode> folderdbNodes = new ArrayList<RepositoryNode>();

    public static boolean setParentNodesOverDatabaseNode(RepositoryNode databaseNode) {
        folderdbNodes.clear();
        RepositoryNode parentNode = databaseNode.getParent();
        if (parentNode == null) {
            return false;
        } else if (parentNode.getProperties(EProperties.CONTENT_TYPE).equals(RepositoryNodeType.FOLDER)) {
            folderdbNodes.add(parentNode);
        }
        return setParentNodesOverDatabaseNode(parentNode);
    }

    public static List<RepositoryNode> getFolderdbNodes() {
        return folderdbNodes;
    }

    public static void setFolderdbNodes(List<RepositoryNode> folderdbNodes) {
        EMFRepositoryNodeManager.folderdbNodes = folderdbNodes;
    }

}
