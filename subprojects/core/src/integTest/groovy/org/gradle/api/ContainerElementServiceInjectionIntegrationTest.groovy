/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api

import org.gradle.api.internal.GeneratedSubclass
import org.gradle.api.plugins.ExtensionAware
import org.gradle.integtests.fixtures.AbstractIntegrationSpec


class ContainerElementServiceInjectionIntegrationTest extends AbstractIntegrationSpec {
    // Document current behaviour
    def "container element can receive services through constructor and is not annotated with @Inject"() {
        buildFile << """
            class Bean {
                String name
                
                Bean(String name, ObjectFactory factory) {
                    println(factory != null ? "got it" : "NOT IT")
                    this.name = name

                    // is not generated
                    assert getClass() == Bean
                }
            }
            
            def container = project.container(Bean)
            container.create("one") {
                assert name == "one"
            }
        """

        expect:
        succeeds()
        outputContains("got it")
    }

    def "fails when container element requests unknown service"() {
        buildFile << """
            interface Unknown { }
            
            class Bean {
                String name
                
                Bean(String name, Unknown thing) {
                }
            }
            
            def container = project.container(Bean)
            container.create("one") {
                assert name == "one"
            }
        """

        expect:
        fails()
        failure.assertHasCause("Could not create an instance of type Bean.")
        failure.assertHasCause("Unable to determine constructor argument #2: missing parameter of type Unknown, or no service of type Unknown")
    }

    def "container element can receive services through getter method"() {
        buildFile << """
            class Bean {
                String name
                
                Bean(String name) {
                    println(factory != null ? "got it" : "NOT IT")
                    this.name = name

                    // is generated but not extensible
                    assert getClass() != Bean
                    assert (this instanceof ${GeneratedSubclass.name}) 
                    assert !(this instanceof ${ExtensionAware.name}) 
                }
                
                @javax.inject.Inject
                ObjectFactory getFactory() { null }
            }
            
            def container = project.container(Bean)
            container.create("one") {
                assert name == "one"
            }
        """

        expect:
        succeeds()
        outputContains("got it")
    }

    def "container element can receive services through abstract getter method"() {
        buildFile << """
            abstract class Bean {
                String name
                
                Bean(String name) {
                    println(factory != null ? "got it" : "NOT IT")
                    this.name = name

                    // is generated but not extensible
                    assert getClass() != Bean
                    assert (this instanceof ${GeneratedSubclass.name}) 
                    assert !(this instanceof ${ExtensionAware.name}) 
                }
                
                @javax.inject.Inject
                abstract ObjectFactory getFactory()
            }
            
            def container = project.container(Bean)
            container.create("one") {
                assert name == "one"
            }
        """

        expect:
        succeeds()
        outputContains("got it")
    }
}
