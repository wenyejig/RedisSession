package com.radiadesign.catalina.session;

import java.io.IOException;
import javax.servlet.http.HttpSession;

public abstract interface Serializer
{
  public abstract void setClassLoader(ClassLoader paramClassLoader);

  public abstract byte[] serializeFrom(HttpSession paramHttpSession)
    throws IOException;

  public abstract HttpSession deserializeInto(byte[] paramArrayOfByte, HttpSession paramHttpSession)
    throws IOException, ClassNotFoundException;
}

/* Location:           /Users/wench/code/apache-tomcat-session/lib/tomcat-redis-session-manager-1.2-tomcat-7-java-7-1.0.jar
 * Qualified Name:     com.radiadesign.catalina.session.Serializer
 * JD-Core Version:    0.6.2
 */