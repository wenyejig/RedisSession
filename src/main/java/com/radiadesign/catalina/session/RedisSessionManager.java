/*     */ package com.radiadesign.catalina.session;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import java.io.PrintStream;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Arrays;
/*     */ import java.util.Collections;
/*     */ import java.util.Iterator;
/*     */ import java.util.Set;
/*     */ import org.apache.catalina.Container;
/*     */ import org.apache.catalina.Lifecycle;
/*     */ import org.apache.catalina.LifecycleException;
/*     */ import org.apache.catalina.LifecycleListener;
/*     */ import org.apache.catalina.LifecycleState;
/*     */ import org.apache.catalina.Loader;
/*     */ import org.apache.catalina.Pipeline;
/*     */ import org.apache.catalina.Session;
/*     */ import org.apache.catalina.Valve;
/*     */ import org.apache.catalina.session.ManagerBase;
/*     */ import org.apache.catalina.util.LifecycleSupport;
/*     */ import org.apache.juli.logging.Log;
/*     */ import org.apache.juli.logging.LogFactory;
/*     */ import redis.clients.jedis.Jedis;
/*     */ import redis.clients.jedis.JedisPool;
/*     */ import redis.clients.jedis.JedisPoolConfig;
/*     */ 
/*     */ public class RedisSessionManager extends ManagerBase
/*     */   implements Lifecycle
/*     */ {
/*  21 */   protected byte[] NULL_SESSION = "null".getBytes();
/*     */ 
/*  23 */   private final Log log = LogFactory.getLog(RedisSessionManager.class);
/*     */ 
/*  25 */   protected String host = "localhost";
/*  26 */   protected int port = 6379;
/*  27 */   protected int database = 0;
/*  28 */   protected String password = null;
/*  29 */   protected int timeout = 2000;
/*     */   protected JedisPool connectionPool;
/*     */   protected RedisSessionHandlerValve handlerValve;
/*  33 */   protected ThreadLocal<RedisSession> currentSession = new ThreadLocal();
/*  34 */   protected ThreadLocal<String> currentSessionId = new ThreadLocal();
/*  35 */   protected ThreadLocal<Boolean> currentSessionIsPersisted = new ThreadLocal();
/*     */   protected Serializer serializer;
/*  38 */   protected static String name = "RedisSessionManager";
/*     */ 
/*  40 */   protected String serializationStrategyClass = "com.radiadesign.catalina.session.JavaSerializer";
/*     */ 
/*  45 */   protected LifecycleSupport lifecycle = new LifecycleSupport(this);
/*     */ 
/*     */   public String getHost() {
/*  48 */     return this.host;
/*     */   }
/*     */ 
/*     */   public void setHost(String host) {
/*  52 */     this.host = host;
/*     */   }
/*     */ 
/*     */   public int getPort() {
/*  56 */     return this.port;
/*     */   }
/*     */ 
/*     */   public void setPort(int port) {
/*  60 */     this.port = port;
/*     */   }
/*     */ 
/*     */   public int getDatabase() {
/*  64 */     return this.database;
/*     */   }
/*     */ 
/*     */   public void setDatabase(int database) {
/*  68 */     this.database = database;
/*     */   }
/*     */ 
/*     */   public int getTimeout() {
/*  72 */     return this.timeout;
/*     */   }
/*     */ 
/*     */   public void setTimeout(int timeout) {
/*  76 */     this.timeout = timeout;
/*     */   }
/*     */ 
/*     */   public String getPassword() {
/*  80 */     return this.password;
/*     */   }
/*     */ 
/*     */   public void setPassword(String password) {
/*  84 */     this.password = password;
/*     */   }
/*     */ 
/*     */   public void setSerializationStrategyClass(String strategy) {
/*  88 */     this.serializationStrategyClass = strategy;
/*     */   }
/*     */ 
/*     */   public int getRejectedSessions()
/*     */   {
/*  93 */     return 0;
/*     */   }
/*     */ 
/*     */   public void setRejectedSessions(int i)
/*     */   {
/*     */   }
/*     */ 
/*     */   protected Jedis acquireConnection() {
/* 101 */     Jedis jedis = this.connectionPool.getResource();
/*     */ 
/* 103 */     if (getDatabase() != 0) {
/* 104 */       jedis.select(getDatabase());
/*     */     }
/*     */ 
/* 107 */     return jedis;
/*     */   }
/*     */ 
/*     */   protected void returnConnection(Jedis jedis, Boolean error) {
/* 111 */     if (error.booleanValue())
/* 112 */       this.connectionPool.returnBrokenResource(jedis);
/*     */     else
/* 114 */       this.connectionPool.returnResource(jedis);
/*     */   }
/*     */ 
/*     */   protected void returnConnection(Jedis jedis)
/*     */   {
/* 119 */     returnConnection(jedis, Boolean.valueOf(false));
/*     */   }
/*     */ 
/*     */   public void load()
/*     */     throws ClassNotFoundException, IOException
/*     */   {
/*     */   }
/*     */ 
/*     */   public void unload()
/*     */     throws IOException
/*     */   {
/*     */   }
/*     */ 
/*     */   public void addLifecycleListener(LifecycleListener listener)
/*     */   {
/* 136 */     this.lifecycle.addLifecycleListener(listener);
/*     */   }
/*     */ 
/*     */   public LifecycleListener[] findLifecycleListeners()
/*     */   {
/* 144 */     return this.lifecycle.findLifecycleListeners();
/*     */   }
/*     */ 
/*     */   public void removeLifecycleListener(LifecycleListener listener)
/*     */   {
/* 154 */     this.lifecycle.removeLifecycleListener(listener);
/*     */   }
/*     */ 
/*     */   protected synchronized void startInternal()
/*     */     throws LifecycleException
/*     */   {
/* 166 */     super.startInternal();
/*     */ 
/* 168 */     setState(LifecycleState.STARTING);
/*     */ 
/* 170 */     Boolean attachedToValve = Boolean.valueOf(false);
/* 171 */     for (Valve valve : getContainer().getPipeline().getValves()) {
/* 172 */       if ((valve instanceof RedisSessionHandlerValve)) {
/* 173 */         this.handlerValve = ((RedisSessionHandlerValve)valve);
/* 174 */         this.handlerValve.setRedisSessionManager(this);
/* 175 */         this.log.info("Attached to RedisSessionHandlerValve");
/* 176 */         attachedToValve = Boolean.valueOf(true);
/* 177 */         break;
/*     */       }
/*     */     }
/*     */ 
/* 181 */     if (!attachedToValve.booleanValue()) {
/* 182 */       String error = "Unable to attach to session handling valve; sessions cannot be saved after the request without the valve starting properly.";
/* 183 */       this.log.fatal(error);
/* 184 */       throw new LifecycleException(error);
/*     */     }
/*     */     try
/*     */     {
/* 188 */       initializeSerializer();
/*     */     } catch (ClassNotFoundException|InstantiationException|IllegalAccessException e) {
/* 190 */       this.log.fatal("Unable to load serializer", e);
/* 191 */       throw new LifecycleException(e);
/*     */     }
/*     */ 
/* 194 */     this.log.info("Will expire sessions after " + getMaxInactiveInterval() + " seconds");
/*     */ 
/* 196 */     initializeDatabaseConnection();
/*     */ 
/* 198 */     setDistributable(true);
/*     */   }
/*     */ 
/*     */   protected synchronized void stopInternal()
/*     */     throws LifecycleException
/*     */   {
/* 211 */     if (this.log.isDebugEnabled()) {
/* 212 */       this.log.debug("Stopping");
/*     */     }
/*     */ 
/* 215 */     setState(LifecycleState.STOPPING);
/*     */     try
/*     */     {
/* 218 */       this.connectionPool.destroy();
/*     */     }
/*     */     catch (Exception localException)
/*     */     {
/*     */     }
/* 223 */     Session[] sessions = findSessions();
/* 224 */     for (int i = 0; i < sessions.length; i++) {
/* 225 */       Session session = sessions[i];
/*     */       try {
/* 227 */         if (session.isValid())
/* 228 */           session.expire();
/*     */       }
/*     */       catch (Throwable localThrowable)
/*     */       {
/*     */       }
/*     */       finally
/*     */       {
/* 235 */         session.recycle();
/*     */       }
/*     */     }
/*     */ 
/* 239 */     super.stopInternal();
/*     */   }
/*     */ 
/*     */   public Session createSession(String sessionId)
/*     */   {
/* 244 */     RedisSession session = (RedisSession)createEmptySession();
/*     */ 
/* 247 */     session.setNew(true);
/* 248 */     session.setValid(true);
/* 249 */     session.setCreationTime(System.currentTimeMillis());
/* 250 */     session.setMaxInactiveInterval(getMaxInactiveInterval());
/*     */ 
/* 252 */     String jvmRoute = getJvmRoute();
/*     */ 
/* 254 */     Boolean error = Boolean.valueOf(true);
/* 255 */     Jedis jedis = null;
/*     */     try
/*     */     {
/* 258 */       jedis = acquireConnection();
/*     */       do
/*     */       {
/* 262 */         if (null == sessionId) {
/* 263 */           sessionId = generateSessionId();
/*     */         }
/*     */ 
/* 266 */         if (jvmRoute != null)
/* 267 */           sessionId = sessionId + '.' + jvmRoute;
/*     */       }
/* 269 */       while (jedis.setnx(sessionId.getBytes(), this.NULL_SESSION).longValue() == 1L);
/*     */ 
/* 277 */       error = Boolean.valueOf(false);
/*     */ 
/* 279 */       session.setId(sessionId);
/* 280 */       session.tellNew();
/*     */ 
/* 282 */       this.currentSession.set(session);
/* 283 */       this.currentSessionId.set(sessionId);
/* 284 */       this.currentSessionIsPersisted.set(Boolean.valueOf(false));
/*     */     } finally {
/* 286 */       if (jedis != null) {
/* 287 */         returnConnection(jedis, error);
/*     */       }
/*     */     }
/*     */ 
/* 291 */     return session;
/*     */   }
/*     */ 
/*     */   public Session createEmptySession()
/*     */   {
/* 296 */     return new RedisSession(this);
/*     */   }
/*     */ 
/*     */   public void add(Session session)
/*     */   {
/*     */     try {
/* 302 */       save(session);
/*     */     } catch (IOException ex) {
/* 304 */       this.log.warn("Unable to add to session manager store: " + ex.getMessage());
/* 305 */       throw new RuntimeException("Unable to add to session manager store.", ex);
/*     */     }
/*     */   }
/*     */ 
/*     */   public Session findSession(String id)
/*     */     throws IOException
/*     */   {
/*     */     RedisSession session=null;
/* 313 */     if (id == null) {
/* 315 */       this.currentSessionIsPersisted.set(Boolean.valueOf(false));
/*     */     }
/*     */     else
/*     */     {
/* 316 */       if (id.equals(this.currentSessionId.get())) {
/* 317 */         session = (RedisSession)this.currentSession.get();
/*     */       } else {
/* 319 */         session = loadSessionFromRedis(id);
/*     */ 
/* 321 */         if (session != null) {
/* 322 */           this.currentSessionIsPersisted.set(Boolean.valueOf(true));
/*     */         }
/*     */       }
/*     */     }
/* 326 */     this.currentSession.set(session);
/* 327 */     this.currentSessionId.set(id);
/*     */ 
/* 329 */     return session;
/*     */   }
/*     */ 
/*     */   public void clear() {
/* 333 */     Jedis jedis = null;
/* 334 */     Boolean error = Boolean.valueOf(true);
/*     */     try {
/* 336 */       jedis = acquireConnection();
/* 337 */       jedis.flushDB();
/* 338 */       error = Boolean.valueOf(false);
/*     */ 
/* 340 */       if (jedis != null)
/* 341 */         returnConnection(jedis, error);
/*     */     }
/*     */     finally
/*     */     {
/* 340 */       if (jedis != null)
/* 341 */         returnConnection(jedis, error);
/*     */     }
/*     */   }
/*     */ 
/*     */   public int getSize() throws IOException
/*     */   {
/* 347 */     Jedis jedis = null;
/* 348 */     Boolean error = Boolean.valueOf(true);
/*     */     try {
/* 350 */       jedis = acquireConnection();
/* 351 */       int size = jedis.dbSize().intValue();
/* 352 */       error = Boolean.valueOf(false);
/* 353 */       return size;
/*     */     } finally {
/* 355 */       if (jedis != null)
/* 356 */         returnConnection(jedis, error);
/*     */     }
/*     */   }
/*     */ 
/*     */   public String[] keys() throws IOException
/*     */   {
/* 362 */     Jedis jedis = null;
/* 363 */     Boolean error = Boolean.valueOf(true);
/*     */     try {
/* 365 */       jedis = acquireConnection();
/* 366 */       Set keySet = jedis.keys("*");
/* 367 */       error = Boolean.valueOf(false);
/* 368 */       return (String[])keySet.toArray(new String[keySet.size()]);
/*     */     } finally {
/* 370 */       if (jedis != null)
/* 371 */         returnConnection(jedis, error);
/*     */     }
/*     */   }
/*     */ 
/*     */   public RedisSession loadSessionFromRedis(String id)
/*     */     throws IOException
/*     */   {
/* 379 */     Jedis jedis = null;
/* 380 */     Boolean error = Boolean.valueOf(true);
/*     */     try
/*     */     {
/* 383 */       this.log.trace("Attempting to load session " + id + " from Redis");
/*     */ 
/* 385 */       jedis = acquireConnection();
/* 386 */       byte[] data = jedis.get(id.getBytes());
/* 387 */       error = Boolean.valueOf(false);
/*     */       RedisSession session;
/*     */       Object localObject1;
/* 389 */       if (data == null) {
/* 390 */         this.log.trace("Session " + id + " not found in Redis");
/* 391 */         session = null; } else {
/* 392 */         if (Arrays.equals(this.NULL_SESSION, data)) {
/* 393 */           throw new IllegalStateException("Race condition encountered: attempted to load session[" + id + "] which has been created but not yet serialized.");
/*     */         }
/* 395 */         this.log.trace("Deserializing session " + id + " from Redis");
/* 396 */         session = (RedisSession)createEmptySession();
/* 397 */         this.serializer.deserializeInto(data, session);
/* 398 */         session.setId(id);
/* 399 */         session.setNew(false);
/* 400 */         session.setMaxInactiveInterval(getMaxInactiveInterval() * 1000);
/* 401 */         session.access();
/* 402 */         session.setValid(true);
/* 403 */         session.resetDirtyTracking();
/*     */ 
/* 405 */         if (this.log.isTraceEnabled()) {
/* 406 */           this.log.trace("Session Contents [" + id + "]:");
/* 407 */           for (localObject1 = Collections.list(session.getAttributeNames()).iterator(); ((Iterator)localObject1).hasNext(); ) { Object name = ((Iterator)localObject1).next();
/* 408 */             this.log.trace("  " + name);
/*     */           }
/*     */         }
/*     */       }
/*     */ 
/* 413 */       return session;
/*     */     } catch (IOException e) {
/* 415 */       this.log.fatal(e.getMessage());
/* 416 */       throw e;
/*     */     } catch (ClassNotFoundException ex) {
/* 418 */       this.log.fatal("Unable to deserialize into session", ex);
/* 419 */       throw new IOException("Unable to deserialize into session", ex);
/*     */     } finally {
/* 421 */       if (jedis != null)
/* 422 */         returnConnection(jedis, error);
/*     */     }
/*     */   }
/*     */ 
/*     */   public void save(Session session) throws IOException
/*     */   {
/* 428 */     Jedis jedis = null;
/* 429 */     Boolean error = Boolean.valueOf(true);
/*     */     try
/*     */     {
/* 432 */       this.log.trace("Saving session " + session + " into Redis");
/*     */ 
/* 434 */       RedisSession redisSession = (RedisSession)session;
/*     */       Iterator localIterator;
/* 436 */       if (this.log.isTraceEnabled()) {
/* 437 */         this.log.trace("Session Contents [" + redisSession.getId() + "]:");
/* 438 */         for (localIterator = Collections.list(redisSession.getAttributeNames()).iterator(); localIterator.hasNext(); ) { Object name = localIterator.next();
/* 439 */           this.log.trace("  " + name);
/*     */         }
/*     */       }
/*     */ 
/* 443 */       Boolean sessionIsDirty = redisSession.isDirty();
/*     */ 
/* 445 */       redisSession.resetDirtyTracking();
/* 446 */       byte[] binaryId = redisSession.getId().getBytes();
/*     */ 
/* 448 */       jedis = acquireConnection();
/*     */ 
/* 450 */       if ((sessionIsDirty.booleanValue()) || (((Boolean)this.currentSessionIsPersisted.get()).booleanValue() != true)) {
/* 451 */         jedis.set(binaryId, this.serializer.serializeFrom(redisSession));
/*     */       }
/*     */ 
/* 454 */       this.currentSessionIsPersisted.set(Boolean.valueOf(true));
/*     */ 
/* 456 */       this.log.trace("Setting expire timeout on session [" + redisSession.getId() + "] to " + getMaxInactiveInterval());
/* 457 */       jedis.expire(binaryId, getMaxInactiveInterval());
/*     */ 
/* 459 */       error = Boolean.valueOf(false);
/*     */     } catch (IOException e) {
/* 461 */       this.log.error(e.getMessage());
/*     */ 
/* 463 */       throw e;
/*     */     } finally {
/* 465 */       if (jedis != null)
/* 466 */         returnConnection(jedis, error);
/*     */     }
/*     */   }
/*     */ 
/*     */   public void remove(Session session)
/*     */   {
/* 472 */     remove(session, false);
/*     */   }
/*     */ 
/*     */   public String getSessionAttribute(String sessionId, String key)
/*     */   {
/* 477 */     return super.getSessionAttribute(sessionId, key);
/*     */   }
/*     */ 
/*     */   public void remove(Session session, boolean update) {
/* 481 */     Jedis jedis = null;
/* 482 */     Boolean error = Boolean.valueOf(true);
/*     */ 
/* 484 */     this.log.trace("Removing session ID : " + session.getId());
/*     */     try
/*     */     {
/* 487 */       jedis = acquireConnection();
/* 488 */       jedis.del(session.getId());
/* 489 */       error = Boolean.valueOf(false);
/* 490 */       System.out.println("执行session.expire()");
/* 491 */       session.expire();
/*     */     } finally {
/* 493 */       if (jedis != null)
/* 494 */         returnConnection(jedis, error);
/*     */     }
/*     */   }
/*     */ 
/*     */   public void afterRequest()
/*     */   {
/* 501 */     RedisSession redisSession = (RedisSession)this.currentSession.get();
/* 502 */     if (redisSession != null) {
/* 503 */       this.currentSession.remove();
/* 504 */       this.currentSessionId.remove();
/* 505 */       this.currentSessionIsPersisted.remove();
/* 506 */       this.log.trace("Session removed from ThreadLocal :" + redisSession.getIdInternal());
/*     */     }
/*     */   }
/*     */ 
/*     */   public void processExpires()
/*     */   {
/* 514 */     long timeNow = System.currentTimeMillis();
/* 515 */     Session[] sessions = findSessions();
/* 516 */     int expireHere = 0;
/*     */ 
/* 518 */     if (this.log.isDebugEnabled())
/* 519 */       this.log.debug("Start expire sessions " + getName() + " at " + timeNow + " sessioncount " + sessions.length);
/* 520 */     for (int i = 0; i < sessions.length; i++) {
/* 521 */       if ((sessions[i] != null) && (!sessions[i].isValid())) {
/* 522 */         expireHere++;
/*     */       }
/*     */     }
/* 525 */     long timeEnd = System.currentTimeMillis();
/* 526 */     if (this.log.isDebugEnabled())
/* 527 */       this.log.debug("End expire sessions " + getName() + " processingTime " + (timeEnd - timeNow) + " expired sessions: " + expireHere);
/* 528 */     this.processingTime += timeEnd - timeNow;
/*     */   }
/*     */ 
/*     */   private void initializeDatabaseConnection()
/*     */     throws LifecycleException
/*     */   {
/*     */     try
/*     */     {
/* 536 */       this.connectionPool = new JedisPool(new JedisPoolConfig(), getHost(), getPort(), getTimeout(), getPassword());
/*     */     } catch (Exception e) {
/* 538 */       e.printStackTrace();
/* 539 */       throw new LifecycleException("Error Connecting to Redis", e);
/*     */     }
/*     */   }
/*     */ 
/*     */   private void initializeSerializer() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
/* 544 */     this.log.info("Attempting to use serializer :" + this.serializationStrategyClass);
/* 545 */     this.serializer = ((Serializer)Class.forName(this.serializationStrategyClass).newInstance());
/*     */ 
/* 547 */     Loader loader = null;
/*     */ 
/* 549 */     if (this.container != null) {
/* 550 */       loader = this.container.getLoader();
/*     */     }
/*     */ 
/* 553 */     ClassLoader classLoader = null;
/*     */ 
/* 555 */     if (loader != null) {
/* 556 */       classLoader = loader.getClassLoader();
/*     */     }
/* 558 */     this.serializer.setClassLoader(classLoader);
/*     */   }
/*     */ }

/* Location:           /Users/wench/code/apache-tomcat-session/lib/tomcat-redis-session-manager-1.2-tomcat-7-java-7-1.0.jar
 * Qualified Name:     com.radiadesign.catalina.session.RedisSessionManager
 * JD-Core Version:    0.6.2
 */