/*
 * Copyright (c) 2017-2018 Aion foundation.
 *
 *     This file is part of the aion network project.
 *
 *     The aion network project is free software: you can redistribute it
 *     and/or modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation, either version 3 of
 *     the License, or any later version.
 *
 *     The aion network project is distributed in the hope that it will
 *     be useful, but WITHOUT ANY WARRANTY; without even the implied
 *     warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *     See the GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with the aion network project source files.
 *     If not, see <https://www.gnu.org/licenses/>.
 *
 * Contributors:
 *     Aion foundation.
 */

package org.aion.api.server.http;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.aion.api.server.rpc.RpcProcessor;

public abstract class RpcServer {

    protected String hostName;
    protected int port;

    protected boolean corsEnabled;
    protected String corsOrigin;

    protected RpcProcessor rpcProcessor;

    protected boolean sslEnabled;
    protected String sslCertCanonicalPath;
    protected char[] sslCertPass;

    protected boolean stuckThreadDetectorEnabled;

    /**
     * to explicitly force any subclasses to check for null values, access to the following
     * variables is restricted through protected accessor methods
     */
    private Integer workerPoolSize;
    private Integer ioPoolSize;
    private Integer requestQueueSize;

    protected RpcServer(RpcServerBuilder<?> builder) {
        // everything exposed by the builder is immutable, except for the List<String> & char[] sslCertPass
        // 1. List<String> enabledEndpoints - defensively copy
        // 2. char[] sslCertPass - we want to mutate it later ourselves, so store original reference

        hostName = Objects.requireNonNull(builder.hostName);
        port = Objects.requireNonNull(builder.port);

        corsEnabled = builder.corsEnabled;
        corsOrigin = builder.corsOrigin;

        List<String> enabledEndpoints = Collections
            .unmodifiableList(Objects.requireNonNull(builder.enabledEndpoints));
        List<String> enabledMethods = Collections
            .unmodifiableList(Objects.requireNonNull(builder.enabledMethods));
        List<String> disabledMethods = Collections
            .unmodifiableList(Objects.requireNonNull(builder.disabledMethods));
        rpcProcessor = new RpcProcessor(enabledEndpoints, enabledMethods, disabledMethods);

        sslEnabled = builder.sslEnabled;
        if (sslEnabled) {
            String certPath = Objects.requireNonNull(builder.sslCertPath);
            try {
                sslCertCanonicalPath = new File(certPath).getCanonicalPath();
            } catch (Exception e) {
                // rethrow checked exceptions from File
                throw new RuntimeException("Could not locate SSL file at path: " + certPath);
            }
            Objects.requireNonNull(sslCertCanonicalPath); // paranoid check

            //we want to mutate it later ourselves, so store original reference
            sslCertPass = Objects.requireNonNull(builder.sslCertPass);
        }

        // if worker & io pool size is null => select best size based on system
        workerPoolSize = builder.workerPoolSize;
        ioPoolSize = builder.ioPoolSize;
        requestQueueSize = builder.requestQueueSize;
        stuckThreadDetectorEnabled = builder.stuckThreadDetectorEnabled;
    }

    // want to explicitly force user of this class to check for null values here.
    protected Optional<Integer> getWorkerPoolSize() {
        return Optional.ofNullable(workerPoolSize);
    }

    protected Optional<Integer> getIoPoolSize() {
        return Optional.ofNullable(ioPoolSize);
    }

    protected Optional<Integer> getRequestQueueSize() {
        return Optional.ofNullable(requestQueueSize);
    }

    public abstract void start();

    public abstract void stop();
}
