package esmLoader.common;

public class PluginException extends Exception
{

	public PluginException()
	{
	}

	public PluginException(String exceptionMsg)
	{
		super(exceptionMsg);
	}

	public PluginException(String exceptionMsg, Throwable cause)
	{
		super(exceptionMsg, cause);
	}
}
