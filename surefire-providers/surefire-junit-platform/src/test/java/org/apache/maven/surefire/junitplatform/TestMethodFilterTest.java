package org.apache.maven.surefire.junitplatform;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.apache.maven.surefire.api.testset.TestListResolver.toClassFileName;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.apache.maven.surefire.api.testset.TestListResolver;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.UniqueId;
import org.mockito.Mockito;

/**
 * Unit tests for {@link TestMethodFilter}.
 *
 * @since 2.22.0
 */
public class TestMethodFilterTest
{
    private static final ConfigurationParameters CONFIG_PARAMS = mock( ConfigurationParameters.class );

    private static final JupiterConfiguration JUPITER_CONFIG = mock( JupiterConfiguration.class );

    private final TestListResolver resolver = mock( TestListResolver.class );

    private final TestMethodFilter filter = new TestMethodFilter( this.resolver );

    @BeforeClass
    public static void beforeClasss()
    {
        Mockito.when( JUPITER_CONFIG.getDefaultDisplayNameGenerator() )
            .thenReturn( new DisplayNameGenerator.Standard() );
    }

    @Test
    public void includesBasedOnTestListResolver()
                    throws Exception
    {
        when( resolver.shouldRun( toClassFileName( TestClass.class ), "testMethod" ) ).thenReturn( true );

        FilterResult result = filter.apply( newTestMethodDescriptor() );

        assertTrue( result.included() );
        assertFalse( result.excluded() );
    }

    @Test
    public void excludesBasedOnTestListResolver()
                    throws Exception
    {
        when( resolver.shouldRun( toClassFileName( TestClass.class ), "testMethod" ) ).thenReturn( false );

        FilterResult result = filter.apply( newTestMethodDescriptor() );

        assertFalse( result.included() );
        assertTrue( result.excluded() );
    }

    @Test
    public void includesTestDescriptorWithClassSource()
    {
        FilterResult result = filter.apply( newClassTestDescriptor() );

        assertTrue( result.included() );
        assertFalse( result.excluded() );
    }

    private static TestMethodTestDescriptor newTestMethodDescriptor()
                    throws Exception
    {
        UniqueId uniqueId = UniqueId.forEngine( "method" );
        Class<TestClass> testClass = TestClass.class;
        Method testMethod = testClass.getMethod( "testMethod" );
        return new TestMethodTestDescriptor( uniqueId, testClass, testMethod, JUPITER_CONFIG );
    }

    private static ClassTestDescriptor newClassTestDescriptor()
    {
        UniqueId uniqueId = UniqueId.forEngine( "class" );
        return new ClassTestDescriptor( uniqueId, TestClass.class, JUPITER_CONFIG );
    }

    /**
     *
     */
    public static class TestClass
    {
        public void testMethod()
        {
        }
    }
}
