-Dclojure.core.async.pool-size=24
set number of threads


$JAVA_HOME/java -Dclojure.core.async.pool-size=200 -Dlog4j.configuration=File:/opt/equinox/apps/esme/conf/log4j.prop \
                      -cp '/opt/equinox/apps/esme/bin/clj_esmesmpp-1.1.0-SNAPSHOT-standalone.jar:/opt/equinox/apps/esme/libs/*' \
                      esme.core --config=/opt/equinox/apps/esme/conf/esmeprop.conf &
