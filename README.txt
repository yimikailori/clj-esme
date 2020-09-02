-Dclojure.core.async.pool-size=24
set number of threads


$JAVA_HOME/java -Dclojure.core.async.pool-size=200 -Dlog4j.configuration=File:/opt/equinox/apps/esme/conf/log4j.prop \
                      -cp '/opt/equinox/apps/esme/bin/clj_esmesmpp-1.1.0-SNAPSHOT-standalone.jar:/opt/equinox/apps/esme/libs/*' \
                      esme.core --config=/opt/equinox/apps/esme/conf/esmeprop.conf &


 java -Dclojure.core.async.pool-size=200 -Dlogback.configurationFile=/Users/yimika/Documents/IdeaProjects/clj-esme-ng/resources/logback.xml \
        -Dld.esme=/Users/yimika/Documents/IdeaProjects/clj-esme-ng/resources/esmeprop.conf \
        -cp '/Users/yimika/Documents/IdeaProjects/clj-esme-ng/target/clj_esmesmpp-2.1.0-SNAPSHOT-standalone.jar:/Users/yimika/Documents/IdeaProjects/clj-esme-ng/lib/*' \
         esme.core


 web: java \
  -Dld.esme=resources/esmeprop.conf -cp 'target/clj_esmesmpp-2.1.0-SNAPSHOT-standalone.jar:/lib/*' clojure.main -m esme.core

  java -Dld.esme=resources/esmeprop.conf -cp 'target/clj_esmesmpp-2.1.0-SNAPSHOT-standalone.jar:/lib/*' -m esme.core



