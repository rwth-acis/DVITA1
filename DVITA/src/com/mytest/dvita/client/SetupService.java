package com.mytest.dvita.client;

import java.util.LinkedList;

import com.google.gwt.user.client.rpc.RemoteService;
import com.mytest.dvita.shared.SerializablePair;
import com.mytest.dvita.shared.ConfigRawdataShared;
import com.mytest.dvita.shared.ConfigTopicminingShared;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("setup")
public interface SetupService extends RemoteService {
	LinkedList<SerializablePair<ConfigRawdataShared, LinkedList<ConfigTopicminingShared>>> getSetupInformation();
	public ConfigTopicminingShared setUpSession(int analysisID, String user, String pw);
}

