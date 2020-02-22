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

package rethink;

import com.rethinkdb.RethinkDB;
import com.rethinkdb.gen.exc.ReqlOpFailedError;
import com.rethinkdb.net.Connection;
import com.rethinkdb.net.Cursor;
import rethink.annotations.RethinkObject;
import rethink.utils.RethinkMapper;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ChangeFeedWorker<T> implements AutoCloseable {

    private static final long timeout = TimeUnit.SECONDS.toMillis(3);

    private final Thread thread;
    private final AtomicBoolean active = new AtomicBoolean(true);

    public ChangeFeedWorker(
        final Connection conn,
        final Class<T> classType,
        final ChangeFeedListener<T> listener
    ) throws NullPointerException {
        this(
            conn,
            conn.db().orElseThrow(() -> new NullPointerException("No database is being used by the connection.")),
            classType.getDeclaredAnnotation(RethinkObject.class).table(),
            classType,
            listener,
            false
        );
    }

    public ChangeFeedWorker(
        final Connection conn,
        final String table,
        final Class<T> classType,
        final ChangeFeedListener<T> listener
    ) throws NullPointerException {
        this(
            conn,
            conn.db().orElseThrow(() -> new NullPointerException("No database is being used by the connection.")),
            table,
            classType,
            listener,
            false
        );
    }

    public ChangeFeedWorker(
        final Connection conn,
        final String db,
        final String table,
        final Class<T> classType,
        final ChangeFeedListener<T> listener
    ) {
        this(conn, db, table, classType, listener, false);
    }

    public ChangeFeedWorker(
        final Connection conn,
        final String db,
        final String table,
        final Class<T> classType,
        final ChangeFeedListener<T> listener,
        final boolean stopOnTableDrop
    ) {
        RethinkDB r = RethinkDB.r;
        Cursor cursor = r
            .db(db)
            .table(table)
            .changes()
            .optArg("include_initial", true)
            .optArg("include_states", true)
            .optArg("include_types", true)
            .run(conn);

        thread = new Thread(() -> {
            while (active.get()) {
                try {
                    Object change = cursor.next(timeout);

                    if (change == null)
                        continue;

                    HashMap<String, Object> map = (HashMap<String, Object>) change;

                    switch ((String) map.get("type")) {
                        case "state":
                            if (map.get("state").equals("initializing"))
                                listener.onInitializingState();
                            else if (map.get("state").equals("ready"))
                                listener.onReadyState();
                            break;
                        case "add":
                            listener.onAdd(RethinkMapper.convert(map.get("new_val"), classType));
                            break;
                        case "change":
                            listener.onChange(
                                RethinkMapper.convert(map.get("old_val"), classType),
                                RethinkMapper.convert(map.get("new_val"), classType)
                            );
                            break;
                        case "initial":
                            listener.onInitial(RethinkMapper.convert(map.get("new_val"), classType));
                            break;
                        case "remove":
                            listener.onRemove(RethinkMapper.convert(map.get("old_val"), classType));
                            break;
                    }
                } catch (TimeoutException e) { /* Do nothing, here to allow thread closing */
                } catch (ReqlOpFailedError e) { /* The table got dropped */
                    if (stopOnTableDrop)
                        active.set(false);
                }
            }
            cursor.close();
        });
        thread.start();
    }

    @Override
    public void close() throws Exception {
        active.set(false);
        thread.join();
    }
}
