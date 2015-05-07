package com.mytest.dvita.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.mytest.dvita.shared.WordData;
import com.mytest.dvita.shared.WordEvolutionData;

@RemoteServiceRelativePath("word")
public interface WordService extends RemoteService{
	
	WordEvolutionData getWordEvolution(Integer[] wordsId, int topicID) throws IllegalArgumentException;
	WordData bestWords(int topic, int i, long topictime) throws IllegalArgumentException;
	
}