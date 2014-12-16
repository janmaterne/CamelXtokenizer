/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package de.materne.camel.test.xtokenize;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ValidationException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import static org.apache.camel.builder.PredicateBuilder.and;

public class XTokenizeTest extends CamelTestSupport {

    @EndpointInject(uri="mock:valid")
    MockEndpoint valid;

    @EndpointInject(uri="mock:validationError")
    MockEndpoint validationError;



    @Test
    public void validNewsfeed() throws Exception {
        // Newsfeed contains 2 news
        valid.expectedMessageCount(2);
        valid.expectedMessagesMatches(
            // Use helper methods from the static imported PredicateBuilder
            and(
                header("newsfeed.date").isEqualTo("2014.12.09 14:15"),
                header("news.author").isEqualTo("Jan"),
                body().contains("xmlns:news=\"http://www.materne.de/camel/test/xtokenize/\"")
            )                          
        );
        // no error expected
        validationError.expectedMessageCount(0);

        // Read xml from classpath and send to Camel route
        String xml = IOUtils.toString(getClass().getResourceAsStream("/de/materne/camel/test/xtokenize/validNewsfeed.xml"));
        sendBody("direct:in", xml);

        // 'execute' all tests
        assertMockEndpointsSatisfied();
    }



    @Test
    public void invalidXml() throws Exception {
        valid.expectedMessageCount(0);
        validationError.expectedMessageCount(1);

        String xml = "<xml/>";
        sendBody("direct:in", xml);

        assertMockEndpointsSatisfied();
    }



    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // Our XML uses namespaces, so we have to deal with that.
                Namespaces ns = new Namespaces("news", "http://www.materne.de/camel/test/xtokenize/");
                
                // XSD-invalid data goes to this endpoint
                onException(ValidationException.class)
                    .to("mock:validationError");
                
                from("direct:in")

                    // XSD-validation
                    .to("validator:de/materne/camel/test/xtokenize/newsfeed.xsd")
                    
                    // Store newsfeed data in the header before split, so we haven't to do that on each 
                    // splittet news-message.
                    .setHeader("newsfeed.date", ns.xpath("/news:Newsletter/@date", String.class))
                    
                    // http://camel.apache.org/splitter.html
                    // xtokenize() is available since Camel 2.14.
                    // Use the 'wrap'-mode so we keep the Newsletter-Header
                    .split().xtokenize("/news:Newsletter/News", 'w', ns)
                    
                    // Show the splitted message.
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            System.out.println("-- splitted ----------------------------------");
                            System.out.println(exchange.getIn().getBody(String.class));
                            System.out.println("----------------------------------------------");
                        }
                    })
                    
                    // Get some data from the splittet news
                    // XPath access on invalid xml results in an exception!
                    .setHeader("news.date", ns.xpath("/news:Newsletter/News/@date", String.class))
                    .setHeader("news.author", ns.xpath("/news:Newsletter//News/@author", String.class))
                    
                    .to("mock:valid");
            }
        };
    }

}
