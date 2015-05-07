package com.mytest.dvita.client;

import java.util.LinkedList;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.mytest.dvita.shared.SerializablePair;
import com.mytest.dvita.shared.ConfigRawdataShared;
import com.mytest.dvita.shared.ConfigTopicminingShared;

/**
 * The async counterpart of <code>GreetingService</code>.
 */
public interface SetupServiceAsync {

	void getSetupInformation(
			AsyncCallback<LinkedList<SerializablePair<ConfigRawdataShared, LinkedList<ConfigTopicminingShared>>>> asyncCallback);

	void setUpSession(int analysisID, String user, String pw, AsyncCallback<ConfigTopicminingShared> callback);
}
