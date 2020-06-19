package messengers.cormessengers;

import java.util.Collection;

import cor.link.node.Node;
import db.interfaces.IEntity;
import messages.Message;

public class VerifierNode extends Node<Message> {

	/**
	 * 
	 */
	private static final String Verifier = "verifier";

	//private IVerifier _verifier;

	/**
	 *
	 */
	@Override
	public void initialize() throws Exception {
		//_verifier = (IVerifier) _iocContainer.resolve(Verifier);
	}

	/**
	 *
	 */
	@Override
	public boolean execute(Message message) {
		try {
			Collection<IEntity> entities = message.getRequest().getEntities();
			for (IEntity entity : entities) {
				//List<String> issues = _verifier.verify(entity);
			}
			message.getResponse().setPassed(true);
		} catch (Exception e) {
			e.printStackTrace();
			message.getResponse().setPassed(false);
			message.getResponse().setDescription(e.getMessage());
			return false;
		}
		return true;
	}

}
