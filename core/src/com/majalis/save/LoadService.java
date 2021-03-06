package com.majalis.save;
/*
 * Service interface that provides an interface for delivering save messages to the SaveManager
 */
public interface LoadService {
	public <T> T loadDataValue(SaveEnum key, Class<?> type);
	public <T> T loadDataValue(ProfileEnum key, Class<?> type);
}
