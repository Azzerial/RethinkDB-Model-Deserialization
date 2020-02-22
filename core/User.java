/*
 * Copyright 2020 Azzerial
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package core;

import rethink.annotations.RethinkData;
import rethink.annotations.RethinkObject;

import java.util.ArrayList;
import java.util.Arrays;

@RethinkObject(db = "test", table = "users")
public class User {

    /*
     * Json representation and example:
     *
     * {
     *     "id": "a41cc13c-752a-4103-80e7-b9e6963998f1",
     *     "name": "Robin"
     *     "age": 20,
     *     "male": true,
     *     "contact": {
     *         "email": "robin.mercier@epitech.eu",
     *         "website": "azzerial.net"
     *     },
     *     "hobbies": [
     *         "cooking",
     *         "eating",
     *         "programming",
     *         "sleeping"
     *     ]
     * }
     */

    @RethinkData(key = "id")
    private String UUID;
    @RethinkData
    private String name;
    @RethinkData(cast = int.class)
    private int age;
    @RethinkData
    private boolean male;
    @RethinkData
    private Contact contact;
    @RethinkData(cast = String[].class)
    private String[] hobbies;

    public User(String UUID, String name, int age, boolean male, Contact contact, String[] hobbies) {
        this.UUID = UUID;
        this.name = name;
        this.age = age;
        this.male = male;
        this.contact = contact;
        this.hobbies = hobbies;
    }

    public String getUUID() {
        return UUID;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public boolean isMale() {
        return male;
    }

    public Contact getContact() {
        return contact;
    }

    public String[] getHobbies() {
        return hobbies;
    }

    @Override
    public String toString() {
        return "User{" +
            "UUID='" + UUID + '\'' +
            ", name='" + name + '\'' +
            ", age=" + age +
            ", male=" + male +
            ", contact=" + contact +
            ", hobbies=" + Arrays.toString(hobbies) +
            '}';
    }

    @RethinkObject(asRoot = true)
    public static class Contact {

        @RethinkData(path = {"contact"})
        private String website;
        @RethinkData(path = {"contact"})
        private String email;

        public Contact(String website, String email) {
            this.website = website;
            this.email = email;
        }

        public String getWebsite() {
            return website;
        }

        public String getEmail() {
            return email;
        }

        @Override
        public String toString() {
            return "Contact{" +
                "website='" + website + '\'' +
                ", email='" + email + '\'' +
                '}';
        }
    }
}
