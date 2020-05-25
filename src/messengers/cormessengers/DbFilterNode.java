package messengers.cormessengers;

import java.util.List;
import java.util.Map;

import db.IDbComponent;
import db.interfaces.*;
import cor.link.node.Node;
import messages.Message;
import messages.Request;

public class DbFilterNode extends Node<Message> {

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
			IEntity queryEntity = request.getQueryEntity();
			Map<String, String> querySigns = request.getQuerySigns();
			List<IEntity> entities = _dbComponent.filter(queryEntity, querySigns);
			message.getResponse().setIndicator(true);
			message.getResponse().setEntities(entities);
		} catch (Exception e) {
			e.printStackTrace();
			message.getResponse().setIndicator(false);
			message.getResponse().setDescription(e.getMessage());
			return false;
		}
		return true;
	}
}
