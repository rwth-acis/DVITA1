package com.mytest.dvita.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.mytest.dvita.shared.DocumentData;
import com.mytest.dvita.shared.DocumentInfo;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("document")
public interface DocumentService extends RemoteService {
	public DocumentData getDocumentData(int docid) throws IllegalArgumentException;
	DocumentInfo [] relatedDocuments(int TOPICID, int limit, int topictime) throws IllegalArgumentException;
	DocumentInfo [] similarDocuments(int docid, int time) throws IllegalArgumentException;
	
	DocumentInfo[] documentSearch(String string)throws IllegalArgumentException;


}
