package com.mytest.dvita.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.mytest.dvita.shared.ThemeRiverData;
import com.mytest.dvita.shared.TopicLabels;

@RemoteServiceRelativePath("topic")
public interface TopicService extends RemoteService{
	
	ThemeRiverData [] getTopicCurrent(Integer [] Topics) throws IllegalArgumentException;

	TopicLabels getTopicList() throws IllegalArgumentException;
	Integer[] topicSearch(String textitem) throws IllegalArgumentException;

	String [][] getTimeintervals()throws IllegalArgumentException;

	Integer[] topicRanking(int rankType) throws IllegalArgumentException;

	
	
	
}