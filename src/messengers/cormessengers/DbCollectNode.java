package messengers.cormessengers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import db.IDbComponent;
import db.interfaces.*;
import globals.Globals;
import cor.link.node.Node;
import messages.Message;
import messages.Request;
import utilities.Cache;

@SuppressWarnings("unchecked")
public class DbCollectNode extends Node<Message> {

	private static final String DbComponent = "dbComponent";

	private IDbComponent _dbComponent;

	@Override
	public void initialize() throws Exception {
		_dbComponent = (IDbComponent) _iocContainer.resolve(DbComponent);
	}

	@Override
	public boolean execute(Message message) {
		try {
			Request request = message.getRequest();
			String[] tables = request.getTables();
			
			// checking to see if the tables are cached
			Map<String, List<IEntity>> enumTablesMap = (Map<String, List<IEntity>>) Cache.get(Globals.EnumTables);
			if (enumTablesMap == null) {
				Cache.put(Globals.EnumTables, enumTablesMap = new HashMap<String, List<IEntity>>());
			}
			Map<String, List<IEntity>> entitiesMap = new HashMap<String, List<IEntity>>();
			List<String> tablesLeft = new ArrayList<String>();
			for (String table : tables) {
				List<IEntity> tableEntities = enumTablesMap.get(table);
				if (tableEntities == null) {
					tablesLeft.add(table);
				}
				else {
					entitiesMap.put(table, tableEntities);
				}
			}
			// in case any of the tables were not cached
			if (tablesLeft.size() > 0) {
				entitiesMap.putAll(_dbComponent.collect(tablesLeft.toArray(new String[tablesLeft.size()])));
			}
			//returning the collected tables
			message.getResponse().setIndicator(true);
			message.getResponse().setTablesMap(entitiesMap);
		} catch (Exception e) {
			e.printStackTrace();
			message.getResponse().setIndicator(false);
			message.getResponse().setDescription(e.getMessage());
			return false;
		}
		return true;
	}
}
