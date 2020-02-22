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

import com.rethinkdb.RethinkDB;
import com.rethinkdb.net.Connection;
import rethink.ChangeFeedListener;
import rethink.ChangeFeedWorker;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final RethinkDB r = RethinkDB.r;
    private static Connection conn;

    private static void initRethinkDB() {
        conn = r.connection().hostname("localhost").port(28015).connect();

        List<String> databases = r.dbList().run(conn);
        if (!databases.contains("test"))
            r.dbCreate("test").run(conn);

        conn.use("test");

        List<String> tables = r.tableList().run(conn);
        if (!tables.contains("users"))
            r.tableCreate("users").run(conn);
    }

    public static void main(String[] args) {
        initRethinkDB();

        ChangeFeedWorker<User> worker = new ChangeFeedWorker<>(
            conn,
            User.class,
            new ChangeFeedListener<User>() {
                @Override
                public void onInitializingState() {
                    System.out.println("onInitializingState");
                }

                @Override
                public void onReadyState() {
                    System.out.println("onReadyState");
                }

                @Override
                public void onAdd(User value) {
                    System.out.println("onAdd: " + value);
                }

                @Override
                public void onChange(User oldValue, User newValue) {
                    System.out.println("onChange: " + oldValue + " -> " + newValue);
                }

                @Override
                public void onInitial(User value) {
                    System.out.println("onInitial: " + value);
                }

                @Override
                public void onRemove(User value) {
                    System.out.println("onRemove: " + value);
                }
            }
        );

        /* Just here to terminate program after a while */
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(30));
            worker.close();
            conn.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
