/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan;

import java.util.Properties;
import javax.transaction.TransactionManager;

import org.hibernate.boot.internal.SessionFactoryBuilderImpl;
import org.hibernate.boot.internal.SessionFactoryOptionsImpl;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.InfinispanRegionFactory;
import org.hibernate.cache.infinispan.collection.CollectionRegionImpl;
import org.hibernate.cache.infinispan.entity.EntityRegionImpl;
import org.hibernate.cache.infinispan.query.QueryResultsRegionImpl;
import org.hibernate.cache.infinispan.timestamp.TimestampsRegionImpl;
import org.hibernate.cache.infinispan.tm.HibernateTransactionManagerLookup;
import org.hibernate.cache.internal.CacheDataDescriptionImpl;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.transaction.jta.platform.internal.AbstractJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.internal.JBossStandAloneJtaPlatform;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.test.cache.infinispan.util.InfinispanTestingSetup;
import org.hibernate.test.cache.infinispan.util.TestInfinispanRegionFactory;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.boot.ServiceRegistryTestingImpl;
import org.junit.Rule;
import org.junit.Test;

import org.infinispan.AdvancedCache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.TestingUtil;
import org.infinispan.transaction.TransactionMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * InfinispanRegionFactoryTestCase.
 * 
 * @author Galder Zamarre??o
 * @since 3.5
 */
public class InfinispanRegionFactoryTestCase  {
	private static final CacheDataDescription MUTABLE_NON_VERSIONED = new CacheDataDescriptionImpl(true, false, null, null);
	private static final CacheDataDescription IMMUTABLE_NON_VERSIONED = new CacheDataDescriptionImpl(false, false, null, null);

	@Rule
	public InfinispanTestingSetup infinispanTestIdentifier = new InfinispanTestingSetup();

	@Test
	public void testConfigurationProcessing() {
		final String person = "com.acme.Person";
		final String addresses = "com.acme.Person.addresses";
		Properties p = createProperties();
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.cfg", "person-cache");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.eviction.strategy", "LRU");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.eviction.wake_up_interval", "2000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.eviction.max_entries", "5000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.expiration.lifespan", "60000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.expiration.max_idle", "30000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.addresses.cfg", "person-addresses-cache");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.addresses.expiration.lifespan", "120000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.addresses.expiration.max_idle", "60000");
		p.setProperty("hibernate.cache.infinispan.query.cfg", "my-query-cache");
		p.setProperty("hibernate.cache.infinispan.query.eviction.strategy", "LIRS");
		p.setProperty("hibernate.cache.infinispan.query.eviction.wake_up_interval", "3000");
		p.setProperty("hibernate.cache.infinispan.query.eviction.max_entries", "10000");

		InfinispanRegionFactory factory = createRegionFactory(p);

		try {
			assertEquals("entity", factory.getTypeOverrides().get("entity").getCacheName());
			assertEquals("entity", factory.getTypeOverrides().get("collection").getCacheName());
			assertEquals("timestamps", factory.getTypeOverrides().get("timestamps").getCacheName());

			assertEquals("person-cache", factory.getTypeOverrides().get(person).getCacheName());
			assertEquals(EvictionStrategy.LRU, factory.getTypeOverrides().get(person).getEvictionStrategy());
			assertEquals(2000, factory.getTypeOverrides().get(person).getEvictionWakeUpInterval());
			assertEquals(5000, factory.getTypeOverrides().get(person).getEvictionMaxEntries());
			assertEquals(60000, factory.getTypeOverrides().get(person).getExpirationLifespan());
			assertEquals(30000, factory.getTypeOverrides().get(person).getExpirationMaxIdle());

			assertEquals("person-addresses-cache", factory.getTypeOverrides().get(addresses).getCacheName());
			assertEquals(120000, factory.getTypeOverrides().get(addresses).getExpirationLifespan());
			assertEquals(60000, factory.getTypeOverrides().get(addresses).getExpirationMaxIdle());

			assertEquals("my-query-cache", factory.getTypeOverrides().get("query").getCacheName());
			assertEquals(EvictionStrategy.LIRS, factory.getTypeOverrides().get("query").getEvictionStrategy());
			assertEquals(3000, factory.getTypeOverrides().get("query").getEvictionWakeUpInterval());
			assertEquals(10000, factory.getTypeOverrides().get("query").getEvictionMaxEntries());
		} finally {
			factory.stop();
		}
	}

	@Test
	public void testBuildEntityCollectionRegionsPersonPlusEntityCollectionOverrides() {
		final String person = "com.acme.Person";
		final String address = "com.acme.Address";
		final String car = "com.acme.Car";
		final String addresses = "com.acme.Person.addresses";
		final String parts = "com.acme.Car.parts";
		Properties p = createProperties();
		// First option, cache defined for entity and overrides for generic entity data type and entity itself.
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.cfg", "person-cache");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.eviction.strategy", "LRU");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.eviction.wake_up_interval", "2000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.eviction.max_entries", "5000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.expiration.lifespan", "60000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.expiration.max_idle", "30000");
		p.setProperty("hibernate.cache.infinispan.entity.cfg", "myentity-cache");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.strategy", "LIRS");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.wake_up_interval", "3000");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.max_entries", "20000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.addresses.cfg", "addresses-cache");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.addresses.eviction.strategy", "LIRS");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.addresses.eviction.wake_up_interval", "2500");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.addresses.eviction.max_entries", "5500");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.addresses.expiration.lifespan", "65000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.addresses.expiration.max_idle", "35000");
		p.setProperty("hibernate.cache.infinispan.collection.cfg", "mycollection-cache");
		p.setProperty("hibernate.cache.infinispan.collection.eviction.strategy", "LRU");
		p.setProperty("hibernate.cache.infinispan.collection.eviction.wake_up_interval", "3500");
		p.setProperty("hibernate.cache.infinispan.collection.eviction.max_entries", "25000");
		InfinispanRegionFactory factory = createRegionFactory(p);
		try {
			EmbeddedCacheManager manager = factory.getCacheManager();
			assertFalse(manager.getCacheManagerConfiguration()
					.globalJmxStatistics().enabled());
			assertNotNull(factory.getTypeOverrides().get(person));
			assertFalse(factory.getDefinedConfigurations().contains(person));
			assertNotNull(factory.getTypeOverrides().get(addresses));
			assertFalse(factory.getDefinedConfigurations().contains(addresses));
			AdvancedCache cache;

			EntityRegionImpl region = (EntityRegionImpl) factory.buildEntityRegion(person, p, MUTABLE_NON_VERSIONED);
			assertNotNull(factory.getTypeOverrides().get(person));
			assertTrue(factory.getDefinedConfigurations().contains(person));
			assertNull(factory.getTypeOverrides().get(address));
			cache = region.getCache();
			Configuration cacheCfg = cache.getCacheConfiguration();
			assertEquals(EvictionStrategy.LRU, cacheCfg.eviction().strategy());
			assertEquals(2000, cacheCfg.expiration().wakeUpInterval());
			assertEquals(5000, cacheCfg.eviction().maxEntries());
			assertEquals(60000, cacheCfg.expiration().lifespan());
			assertEquals(30000, cacheCfg.expiration().maxIdle());
			assertFalse(cacheCfg.jmxStatistics().enabled());

			region = (EntityRegionImpl) factory.buildEntityRegion(address, p, MUTABLE_NON_VERSIONED);
			assertNotNull(factory.getTypeOverrides().get(person));
			assertTrue(factory.getDefinedConfigurations().contains(person));
			assertNull(factory.getTypeOverrides().get(address));
			cache = region.getCache();
			cacheCfg = cache.getCacheConfiguration();
			assertEquals(EvictionStrategy.LIRS, cacheCfg.eviction().strategy());
			assertEquals(3000, cacheCfg.expiration().wakeUpInterval());
			assertEquals(20000, cacheCfg.eviction().maxEntries());
			assertFalse(cacheCfg.jmxStatistics().enabled());

			region = (EntityRegionImpl) factory.buildEntityRegion(car, p, MUTABLE_NON_VERSIONED);
			assertNotNull(factory.getTypeOverrides().get(person));
			assertTrue(factory.getDefinedConfigurations().contains(person));
			assertNull(factory.getTypeOverrides().get(address));
			cache = region.getCache();
			cacheCfg = cache.getCacheConfiguration();
			assertEquals(EvictionStrategy.LIRS, cacheCfg.eviction().strategy());
			assertEquals(3000, cacheCfg.expiration().wakeUpInterval());
			assertEquals(20000, cacheCfg.eviction().maxEntries());
			assertFalse(cacheCfg.jmxStatistics().enabled());

			CollectionRegionImpl collectionRegion = (CollectionRegionImpl)
					factory.buildCollectionRegion(addresses, p, MUTABLE_NON_VERSIONED);
			assertNotNull(factory.getTypeOverrides().get(addresses));
			assertTrue(factory.getDefinedConfigurations().contains(person));
			assertNull(factory.getTypeOverrides().get(parts));
			cache = collectionRegion .getCache();
			cacheCfg = cache.getCacheConfiguration();
			assertEquals(EvictionStrategy.LIRS, cacheCfg.eviction().strategy());
			assertEquals(2500, cacheCfg.expiration().wakeUpInterval());
			assertEquals(5500, cacheCfg.eviction().maxEntries());
			assertEquals(65000, cacheCfg.expiration().lifespan());
			assertEquals(35000, cacheCfg.expiration().maxIdle());
			assertFalse(cacheCfg.jmxStatistics().enabled());

			collectionRegion = (CollectionRegionImpl) factory.buildCollectionRegion(parts, p, MUTABLE_NON_VERSIONED);
			assertNotNull(factory.getTypeOverrides().get(addresses));
			assertTrue(factory.getDefinedConfigurations().contains(addresses));
			assertNull(factory.getTypeOverrides().get(parts));
			cache = collectionRegion.getCache();
			cacheCfg = cache.getCacheConfiguration();
			assertEquals(EvictionStrategy.LRU, cacheCfg.eviction().strategy());
			assertEquals(3500, cacheCfg.expiration().wakeUpInterval());
			assertEquals(25000, cacheCfg.eviction().maxEntries());
			assertFalse(cacheCfg.jmxStatistics().enabled());

			collectionRegion = (CollectionRegionImpl) factory.buildCollectionRegion(parts, p, MUTABLE_NON_VERSIONED);
			assertNotNull(factory.getTypeOverrides().get(addresses));
			assertTrue(factory.getDefinedConfigurations().contains(addresses));
			assertNull(factory.getTypeOverrides().get(parts));
			cache = collectionRegion.getCache();
			cacheCfg = cache.getCacheConfiguration();
			assertEquals(EvictionStrategy.LRU, cacheCfg.eviction().strategy());
			assertEquals(3500, cacheCfg.expiration().wakeUpInterval());
			assertEquals(25000, cacheCfg.eviction().maxEntries());
			assertFalse(cacheCfg.jmxStatistics().enabled());
		} finally {
			factory.stop();
		}
	}

	@Test
	public void testBuildEntityCollectionRegionOverridesOnly() {
		AdvancedCache cache;
		Properties p = createProperties();
		p.setProperty("hibernate.cache.infinispan.entity.eviction.strategy", "LIRS");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.wake_up_interval", "3000");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.max_entries", "30000");
		p.setProperty("hibernate.cache.infinispan.collection.eviction.strategy", "LRU");
		p.setProperty("hibernate.cache.infinispan.collection.eviction.wake_up_interval", "3500");
		p.setProperty("hibernate.cache.infinispan.collection.eviction.max_entries", "35000");
		InfinispanRegionFactory factory = createRegionFactory(p);
		try {
			factory.getCacheManager();
			EntityRegionImpl region = (EntityRegionImpl) factory.buildEntityRegion("com.acme.Address", p, MUTABLE_NON_VERSIONED);
			assertNull(factory.getTypeOverrides().get("com.acme.Address"));
			cache = region.getCache();
			Configuration cacheCfg = cache.getCacheConfiguration();
			assertEquals(EvictionStrategy.LIRS, cacheCfg.eviction().strategy());
			assertEquals(3000, cacheCfg.expiration().wakeUpInterval());
			assertEquals(30000, cacheCfg.eviction().maxEntries());
			// Max idle value comes from base XML configuration
			assertEquals(100000, cacheCfg.expiration().maxIdle());

			CollectionRegionImpl collectionRegion = (CollectionRegionImpl)
					factory.buildCollectionRegion("com.acme.Person.addresses", p, MUTABLE_NON_VERSIONED);
			assertNull(factory.getTypeOverrides().get("com.acme.Person.addresses"));
			cache = collectionRegion.getCache();
			cacheCfg = cache.getCacheConfiguration();
			assertEquals(EvictionStrategy.LRU, cacheCfg.eviction().strategy());
			assertEquals(3500, cacheCfg.expiration().wakeUpInterval());
			assertEquals(35000, cacheCfg.eviction().maxEntries());
			assertEquals(100000, cacheCfg.expiration().maxIdle());
		} finally {
			factory.stop();
		}
	}
	@Test
	public void testBuildEntityRegionPersonPlusEntityOverridesWithoutCfg() {
		final String person = "com.acme.Person";
		Properties p = createProperties();
		// Third option, no cache defined for entity and overrides for generic entity data type and entity itself.
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.eviction.strategy", "LRU");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.expiration.lifespan", "60000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.expiration.max_idle", "30000");
		p.setProperty("hibernate.cache.infinispan.entity.cfg", "myentity-cache");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.strategy", "FIFO");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.wake_up_interval", "3000");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.max_entries", "10000");
		InfinispanRegionFactory factory = createRegionFactory(p);
		try {
			factory.getCacheManager();
			assertNotNull( factory.getTypeOverrides().get( person ) );
			assertFalse( factory.getDefinedConfigurations().contains( person ) );
			EntityRegionImpl region = (EntityRegionImpl) factory.buildEntityRegion( person, p, MUTABLE_NON_VERSIONED );
			assertNotNull(factory.getTypeOverrides().get(person));
			assertTrue( factory.getDefinedConfigurations().contains( person ) );
			AdvancedCache cache = region.getCache();
			Configuration cacheCfg = cache.getCacheConfiguration();
			assertEquals(EvictionStrategy.LRU, cacheCfg.eviction().strategy());
			assertEquals(3000, cacheCfg.expiration().wakeUpInterval());
			assertEquals(10000, cacheCfg.eviction().maxEntries());
			assertEquals(60000, cacheCfg.expiration().lifespan());
			assertEquals(30000, cacheCfg.expiration().maxIdle());
		} finally {
			factory.stop();
		}
	}

	@Test
	public void testBuildImmutableEntityRegion() {
		AdvancedCache cache;
		Properties p = new Properties();
		InfinispanRegionFactory factory = createRegionFactory(p);
		try {
			factory.getCacheManager();
			EntityRegionImpl region = (EntityRegionImpl) factory.buildEntityRegion("com.acme.Address", p, IMMUTABLE_NON_VERSIONED);
			assertNull( factory.getTypeOverrides().get( "com.acme.Address" ) );
			cache = region.getCache();
			Configuration cacheCfg = cache.getCacheConfiguration();
			assertEquals("Immutable entity should get non-transactional cache", TransactionMode.NON_TRANSACTIONAL, cacheCfg.transaction().transactionMode());
		} finally {
			factory.stop();
		}
	}

	@Test(expected = CacheException.class)
	public void testTimestampValidation() {
		Properties p = createProperties();
		final DefaultCacheManager manager = new DefaultCacheManager(GlobalConfigurationBuilder.defaultClusteredBuilder().build());
		try {
			InfinispanRegionFactory factory = createRegionFactory(manager, p);
			ConfigurationBuilder builder = new ConfigurationBuilder();
			builder.clustering().cacheMode(CacheMode.INVALIDATION_SYNC);
			manager.defineConfiguration( "timestamps", builder.build() );
			factory.start(null, p);
			fail( "Should have failed saying that invalidation is not allowed for timestamp caches." );
		} finally {
			TestingUtil.killCacheManagers( manager );
		}
	}

	@Test
	public void testBuildDefaultTimestampsRegion() {
		final String timestamps = "org.hibernate.cache.spi.UpdateTimestampsCache";
		Properties p = createProperties();
		InfinispanRegionFactory factory = createRegionFactory(p);
		try {
			assertTrue(factory.getDefinedConfigurations().contains("timestamps"));
			assertTrue(factory.getTypeOverrides().get("timestamps")
					.getCacheName().equals("timestamps"));
			TimestampsRegionImpl region = (TimestampsRegionImpl)
					factory.buildTimestampsRegion(timestamps, p);
			AdvancedCache cache = region.getCache();
			Configuration cacheCfg = cache.getCacheConfiguration();
			assertEquals( EvictionStrategy.NONE, cacheCfg.eviction().strategy() );
			assertEquals( CacheMode.REPL_ASYNC, cacheCfg.clustering().cacheMode() );
			assertFalse( cacheCfg.jmxStatistics().enabled() );
		} finally {
			factory.stop();
		}
	}

	@Test
	public void testBuildDiffCacheNameTimestampsRegion() {
		final String timestamps = "org.hibernate.cache.spi.UpdateTimestampsCache";
		Properties p = createProperties();
		p.setProperty("hibernate.cache.infinispan.timestamps.cfg", "unrecommended-timestamps");
		InfinispanRegionFactory factory = createRegionFactory(p);
		try {
			EmbeddedCacheManager manager = factory.getCacheManager();
			assertFalse(factory.getDefinedConfigurations().contains("timestamp"));
			assertTrue(factory.getDefinedConfigurations().contains("unrecommended-timestamps"));
			assertTrue(factory.getTypeOverrides().get("timestamps").getCacheName().equals("unrecommended-timestamps"));
			ConfigurationBuilder builder = new ConfigurationBuilder();
			builder.clustering().stateTransfer().fetchInMemoryState(true);
			builder.clustering().cacheMode( CacheMode.REPL_SYNC );
			manager.defineConfiguration( "unrecommended-timestamps", builder.build() );
			TimestampsRegionImpl region = (TimestampsRegionImpl) factory.buildTimestampsRegion(timestamps, p);
			AdvancedCache cache = region.getCache();
			Configuration cacheCfg = cache.getCacheConfiguration();
			assertEquals(EvictionStrategy.NONE, cacheCfg.eviction().strategy());
			assertEquals(CacheMode.REPL_SYNC, cacheCfg.clustering().cacheMode());
			assertFalse( cacheCfg.storeAsBinary().enabled() );
			assertFalse(cacheCfg.jmxStatistics().enabled());
		} finally {
			factory.stop();
		}
	}

	@Test
	public void testBuildTimestamRegionWithCacheNameOverride() {
		final String timestamps = "org.hibernate.cache.spi.UpdateTimestampsCache";
		Properties p = createProperties();
		p.setProperty("hibernate.cache.infinispan.timestamps.cfg", "mytimestamps-cache");
		InfinispanRegionFactory factory = createRegionFactory(p);
		try {
			factory.buildTimestampsRegion(timestamps, p);
			assertTrue(factory.getDefinedConfigurations().contains("mytimestamps-cache"));
		} finally {
			factory.stop();
		}
	}

	@Test
	public void testBuildTimestamRegionWithFifoEvictionOverride() {
		final String timestamps = "org.hibernate.cache.spi.UpdateTimestampsCache";
		Properties p = createProperties();
		p.setProperty("hibernate.cache.infinispan.timestamps.cfg", "mytimestamps-cache");
		p.setProperty("hibernate.cache.infinispan.timestamps.eviction.strategy", "FIFO");
		p.setProperty("hibernate.cache.infinispan.timestamps.eviction.wake_up_interval", "3000");
		p.setProperty("hibernate.cache.infinispan.timestamps.eviction.max_entries", "10000");
		InfinispanRegionFactory factory = null;
		try {
			factory = createRegionFactory(p);
			factory.buildTimestampsRegion(timestamps, p);
			assertTrue( factory.getDefinedConfigurations().contains( "mytimestamps-cache" ) );
			fail( "Should fail cos no eviction configurations are allowed for timestamp caches" );
		} catch(CacheException ce) {
		} finally {
			if (factory != null) factory.stop();
		}
	}

	@Test
	public void testBuildTimestamRegionWithNoneEvictionOverride() {
		final String timestamps = "org.hibernate.cache.spi.UpdateTimestampsCache";
		Properties p = createProperties();
		p.setProperty("hibernate.cache.infinispan.timestamps.cfg", "timestamps-none-eviction");
		p.setProperty("hibernate.cache.infinispan.timestamps.eviction.strategy", "NONE");
		p.setProperty("hibernate.cache.infinispan.timestamps.eviction.wake_up_interval", "3000");
		p.setProperty("hibernate.cache.infinispan.timestamps.eviction.max_entries", "0");
		InfinispanRegionFactory factory = createRegionFactory(p);
		try {
			factory.buildTimestampsRegion( timestamps, p );
			assertTrue( factory.getDefinedConfigurations().contains( "timestamps-none-eviction" ) );
		} finally {
			factory.stop();
		}
	}

	@Test
	public void testBuildQueryRegion() {
		final String query = "org.hibernate.cache.internal.StandardQueryCache";
		Properties p = createProperties();
		InfinispanRegionFactory factory = createRegionFactory(p);
		try {
			assertTrue(factory.getDefinedConfigurations().contains("local-query"));
			QueryResultsRegionImpl region = (QueryResultsRegionImpl) factory.buildQueryResultsRegion(query, p);
			AdvancedCache cache = region.getCache();
			Configuration cacheCfg = cache.getCacheConfiguration();
			assertEquals( CacheMode.LOCAL, cacheCfg.clustering().cacheMode() );
			assertFalse( cacheCfg.jmxStatistics().enabled() );
		} finally {
			factory.stop();
		}
	}

	@Test
	public void testBuildQueryRegionWithCustomRegionName() {
		final String queryRegionName = "myquery";
		Properties p = createProperties();
		p.setProperty("hibernate.cache.infinispan.myquery.cfg", "timestamps-none-eviction");
		p.setProperty("hibernate.cache.infinispan.myquery.eviction.strategy", "LIRS");
		p.setProperty("hibernate.cache.infinispan.myquery.eviction.wake_up_interval", "2222");
		p.setProperty("hibernate.cache.infinispan.myquery.eviction.max_entries", "11111");
		InfinispanRegionFactory factory = createRegionFactory(p);
		try {
			assertTrue(factory.getDefinedConfigurations().contains("local-query"));
			QueryResultsRegionImpl region = (QueryResultsRegionImpl) factory.buildQueryResultsRegion(queryRegionName, p);
			assertNotNull(factory.getTypeOverrides().get(queryRegionName));
			assertTrue(factory.getDefinedConfigurations().contains(queryRegionName));
			AdvancedCache cache = region.getCache();
			Configuration cacheCfg = cache.getCacheConfiguration();
			assertEquals(EvictionStrategy.LIRS, cacheCfg.eviction().strategy());
			assertEquals(2222, cacheCfg.expiration().wakeUpInterval());
			assertEquals( 11111, cacheCfg.eviction().maxEntries() );
		} finally {
			factory.stop();
		}
	}
	@Test
	public void testEnableStatistics() {
		Properties p = createProperties();
		p.setProperty("hibernate.cache.infinispan.statistics", "true");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.expiration.lifespan", "60000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.expiration.max_idle", "30000");
		p.setProperty("hibernate.cache.infinispan.entity.cfg", "myentity-cache");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.strategy", "FIFO");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.wake_up_interval", "3000");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.max_entries", "10000");
		InfinispanRegionFactory factory = createRegionFactory(p);
		try {
			EmbeddedCacheManager manager = factory.getCacheManager();
			assertTrue(manager.getCacheManagerConfiguration().globalJmxStatistics().enabled());
			EntityRegionImpl region = (EntityRegionImpl) factory.buildEntityRegion("com.acme.Address", p, MUTABLE_NON_VERSIONED);
			AdvancedCache cache = region.getCache();
			assertTrue(factory.getTypeOverrides().get("entity").isExposeStatistics());
			assertTrue(cache.getCacheConfiguration().jmxStatistics().enabled());

			region = (EntityRegionImpl) factory.buildEntityRegion("com.acme.Person", p, MUTABLE_NON_VERSIONED);
			cache = region.getCache();
			assertTrue(factory.getTypeOverrides().get("com.acme.Person").isExposeStatistics());
			assertTrue(cache.getCacheConfiguration().jmxStatistics().enabled());

			final String query = "org.hibernate.cache.internal.StandardQueryCache";
			QueryResultsRegionImpl queryRegion = (QueryResultsRegionImpl)
					factory.buildQueryResultsRegion(query, p);
			cache = queryRegion.getCache();
			assertTrue(factory.getTypeOverrides().get("query").isExposeStatistics());
			assertTrue(cache.getCacheConfiguration().jmxStatistics().enabled());

			final String timestamps = "org.hibernate.cache.spi.UpdateTimestampsCache";
			ConfigurationBuilder builder = new ConfigurationBuilder();
			builder.clustering().stateTransfer().fetchInMemoryState(true);
			manager.defineConfiguration("timestamps", builder.build());
			TimestampsRegionImpl timestampsRegion = (TimestampsRegionImpl)
					factory.buildTimestampsRegion(timestamps, p);
			cache = timestampsRegion.getCache();
			assertTrue(factory.getTypeOverrides().get("timestamps").isExposeStatistics());
			assertTrue(cache.getCacheConfiguration().jmxStatistics().enabled());

			CollectionRegionImpl collectionRegion = (CollectionRegionImpl)
					factory.buildCollectionRegion("com.acme.Person.addresses", p, MUTABLE_NON_VERSIONED);
			cache = collectionRegion.getCache();
			assertTrue(factory.getTypeOverrides().get("collection").isExposeStatistics());
			assertTrue(cache.getCacheConfiguration().jmxStatistics().enabled());
		} finally {
			factory.stop();
		}
	}

	@Test
	public void testDisableStatistics() {
		Properties p = createProperties();
		p.setProperty("hibernate.cache.infinispan.statistics", "false");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.expiration.lifespan", "60000");
		p.setProperty("hibernate.cache.infinispan.com.acme.Person.expiration.max_idle", "30000");
		p.setProperty("hibernate.cache.infinispan.entity.cfg", "myentity-cache");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.strategy", "FIFO");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.wake_up_interval", "3000");
		p.setProperty("hibernate.cache.infinispan.entity.eviction.max_entries", "10000");
		InfinispanRegionFactory factory = createRegionFactory(p);
		try {
			EntityRegionImpl region = (EntityRegionImpl) factory.buildEntityRegion("com.acme.Address", p, MUTABLE_NON_VERSIONED);
			AdvancedCache cache = region.getCache();
			assertFalse( factory.getTypeOverrides().get( "entity" ).isExposeStatistics() );
			assertFalse( cache.getCacheConfiguration().jmxStatistics().enabled() );

			region = (EntityRegionImpl) factory.buildEntityRegion("com.acme.Person", p, MUTABLE_NON_VERSIONED);
			cache = region.getCache();
			assertFalse( factory.getTypeOverrides().get( "com.acme.Person" ).isExposeStatistics() );
			assertFalse( cache.getCacheConfiguration().jmxStatistics().enabled() );

			final String query = "org.hibernate.cache.internal.StandardQueryCache";
			QueryResultsRegionImpl queryRegion = (QueryResultsRegionImpl) factory.buildQueryResultsRegion(query, p);
			cache = queryRegion.getCache();
			assertFalse( factory.getTypeOverrides().get( "query" ).isExposeStatistics() );
			assertFalse( cache.getCacheConfiguration().jmxStatistics().enabled() );

			final String timestamps = "org.hibernate.cache.spi.UpdateTimestampsCache";
			ConfigurationBuilder builder = new ConfigurationBuilder();
			builder.clustering().stateTransfer().fetchInMemoryState(true);
			factory.getCacheManager().defineConfiguration( "timestamps", builder.build() );
			TimestampsRegionImpl timestampsRegion = (TimestampsRegionImpl)
					factory.buildTimestampsRegion(timestamps, p);
			cache = timestampsRegion.getCache();
			assertFalse( factory.getTypeOverrides().get( "timestamps" ).isExposeStatistics() );
			assertFalse( cache.getCacheConfiguration().jmxStatistics().enabled() );

			CollectionRegionImpl collectionRegion = (CollectionRegionImpl)
					factory.buildCollectionRegion("com.acme.Person.addresses", p, MUTABLE_NON_VERSIONED);
			cache = collectionRegion.getCache();
			assertFalse( factory.getTypeOverrides().get( "collection" ).isExposeStatistics() );
			assertFalse( cache.getCacheConfiguration().jmxStatistics().enabled() );
		} finally {
			factory.stop();
		}
	}

	private InfinispanRegionFactory createRegionFactory(Properties p) {
		return createRegionFactory(null, p);
	}

	private InfinispanRegionFactory createRegionFactory(final EmbeddedCacheManager manager, Properties p) {
		final InfinispanRegionFactory factory = new TestInfinispanRegionFactory(new Properties()) {

			@Override
			protected org.infinispan.transaction.lookup.TransactionManagerLookup createTransactionManagerLookup(SessionFactoryOptions settings, Properties properties) {
				return new HibernateTransactionManagerLookup(null, null) {
					@Override
					public TransactionManager getTransactionManager() throws Exception {
						AbstractJtaPlatform jta = new JBossStandAloneJtaPlatform();
						jta.injectServices(ServiceRegistryBuilder.buildServiceRegistry());
						return jta.getTransactionManager();
					}
				};
			}

			@Override
			protected EmbeddedCacheManager createCacheManager(Properties properties, ServiceRegistry serviceRegistry) throws CacheException {
				if (manager != null)
					return manager;
				else
					return super.createCacheManager( properties, serviceRegistry);
			}

		};

		factory.start( sfOptionsForStart(), p );
		return factory;
	}

	private SessionFactoryOptions sfOptionsForStart() {
		return new SessionFactoryOptionsImpl(
				  new SessionFactoryBuilderImpl.SessionFactoryOptionsStateStandardImpl(
							 ServiceRegistryTestingImpl.forUnitTesting()
				  )
		);
	}

	private static Properties createProperties() {
		final Properties properties = new Properties();
		// If configured in the environment, add configuration file name to properties.
		final String cfgFileName =
				  (String) Environment.getProperties().get( InfinispanRegionFactory.INFINISPAN_CONFIG_RESOURCE_PROP );
		if ( cfgFileName != null ) {
			properties.put( InfinispanRegionFactory.INFINISPAN_CONFIG_RESOURCE_PROP, cfgFileName );
		}
		return properties;
	}
}
