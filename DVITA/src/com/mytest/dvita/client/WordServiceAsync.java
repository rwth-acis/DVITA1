package com.mytest.dvita.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.mytest.dvita.shared.WordData;
import com.mytest.dvita.shared.WordEvolutionData;


public interface WordServiceAsync {


	void getWordEvolution(Integer[] wordsId, int topicID, AsyncCallback<WordEvolutionData> asyncCallback) throws IllegalArgumentException;
	void bestWords(int topic, int i, long topictime,
			AsyncCallback<WordData> asyncCallback) throws IllegalArgumentException;


	
}
