/*
 * Copyright 2018-2019 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.service;

public class
AbstractService {
    private final static String webinRestUriTest = "https://wwwdev.ebi.ac.uk/ena/submit/drop-box/";
    private final static String webinRestUriProd = "https://www.ebi.ac.uk/ena/submit/drop-box/";
    private final String userName;
    private final String password;
    private final boolean test;

    final String getWebinRestUri(String uri, boolean test) {
        return (test) ?
                webinRestUriTest + uri :
                webinRestUriProd + uri;
    }

    public abstract static class
    AbstractBuilder<T> {
        protected String userName;
        protected String password;
        protected boolean test;

        public AbstractBuilder<T>
        setUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public AbstractBuilder<T>
        setPassword(String password) {
            this.password = password;
            return this;
        }

        public AbstractBuilder<T>
        setCredentials(String userName, String password) {
            setUserName(userName);
            setPassword(password);
            return this;
        }

        public AbstractBuilder<T>
        setTest(boolean test) {
            this.test = test;
            return this;
        }

        public abstract T build();
    }

    public String
    getUserName() {
        return this.userName;
    }

    protected AbstractService(AbstractBuilder<?> builder) {
        this.userName = builder.userName;
        this.password = builder.password;
        this.test = builder.test;
    }

    public String
    getPassword() {
        return this.password;
    }

    public boolean
    getTest() {
        return this.test;
    }
}
