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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.configuration;

import org.apache.flink.annotation.Internal;
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.annotation.docs.ConfigGroup;
import org.apache.flink.annotation.docs.ConfigGroups;
import org.apache.flink.annotation.docs.Documentation;
import org.apache.flink.configuration.description.Description;
import org.apache.flink.util.ArrayUtils;

import org.apache.flink.shaded.guava33.com.google.common.collect.Iterables;

import java.util.List;

import static org.apache.flink.configuration.ConfigOptions.key;
import static org.apache.flink.configuration.description.TextElement.code;

/** The set of configuration options for core parameters. */
@PublicEvolving
@ConfigGroups(groups = {@ConfigGroup(name = "Environment", keyPrefix = "env")})
public class CoreOptions {

    @Internal
    public static final String[] PARENT_FIRST_LOGGING_PATTERNS =
            new String[] {
                "org.slf4j",
                "org.apache.log4j",
                "org.apache.logging",
                "org.apache.commons.logging",
                "ch.qos.logback"
            };

    // ------------------------------------------------------------------------
    //  Classloading Parameters
    // ------------------------------------------------------------------------

    /**
     * Defines the class resolution strategy when loading classes from user code, meaning whether to
     * first check the user code jar ({@code "child-first"}) or the application classpath ({@code
     * "parent-first"})
     *
     * <p>The default settings indicate to load classes first from the user code jar, which means
     * that user code jars can include and load different dependencies than Flink uses
     * (transitively).
     *
     * <p>Exceptions to the rules are defined via {@link #ALWAYS_PARENT_FIRST_LOADER_PATTERNS}.
     */
    @Documentation.Section(Documentation.Sections.EXPERT_CLASS_LOADING)
    public static final ConfigOption<String> CLASSLOADER_RESOLVE_ORDER =
            ConfigOptions.key("classloader.resolve-order")
                    .stringType()
                    .defaultValue("child-first")
                    .withDescription(
                            "Defines the class resolution strategy when loading classes from user code, meaning whether to"
                                    + " first check the user code jar (\"child-first\") or the application classpath (\"parent-first\")."
                                    + " The default settings indicate to load classes first from the user code jar, which means that user code"
                                    + " jars can include and load different dependencies than Flink uses (transitively).");

    /**
     * The namespace patterns for classes that are loaded with a preference from the parent
     * classloader, meaning the application class path, rather than any user code jar file. This
     * option only has an effect when {@link #CLASSLOADER_RESOLVE_ORDER} is set to {@code
     * "child-first"}.
     *
     * <p>It is important that all classes whose objects move between Flink's runtime and any user
     * code (including Flink connectors that run as part of the user code) are covered by these
     * patterns here. Otherwise, it is possible that the Flink runtime and the user code load two
     * different copies of a class through the different class loaders. That leads to errors like "X
     * cannot be cast to X" exceptions, where both class names are equal, or "X cannot be assigned
     * to Y", where X should be a subclass of Y.
     *
     * <p>The following classes are loaded parent-first, to avoid any duplication:
     *
     * <ul>
     *   <li>All core Java classes (java.*), because they must never be duplicated.
     *   <li>All core Scala classes (scala.*). Currently Scala is used in the Flink runtime and in
     *       the user code, and some Scala classes cross the boundary, such as the <i>FunctionX</i>
     *       classes. That may change if Scala eventually lives purely as part of the user code.
     *   <li>All Flink classes (org.apache.flink.*). Note that this means that connectors and
     *       formats (flink-avro, etc) are loaded parent-first as well if they are in the core
     *       classpath.
     *   <li>Java annotations and loggers, defined by the following list:
     *       javax.annotation;org.slf4j;org.apache.log4j;org.apache.logging;org.apache.commons.logging;ch.qos.logback.
     *       This is done for convenience, to avoid duplication of annotations and multiple log
     *       bindings.
     * </ul>
     */
    @Documentation.Section(Documentation.Sections.EXPERT_CLASS_LOADING)
    public static final ConfigOption<List<String>> ALWAYS_PARENT_FIRST_LOADER_PATTERNS =
            ConfigOptions.key("classloader.parent-first-patterns.default")
                    .stringType()
                    .asList()
                    .defaultValues(
                            ArrayUtils.concat(
                                    new String[] {
                                        "java.",
                                        "scala.",
                                        "org.apache.flink.",
                                        "com.esotericsoftware.kryo",
                                        "org.apache.hadoop.",
                                        "javax.annotation.",
                                        "org.xml",
                                        "javax.xml",
                                        "org.apache.xerces",
                                        "org.w3c",
                                        "org.rocksdb."
                                    },
                                    PARENT_FIRST_LOGGING_PATTERNS))
                    .withDeprecatedKeys("classloader.parent-first-patterns")
                    .withDescription(
                            "A (semicolon-separated) list of patterns that specifies which classes should always be"
                                    + " resolved through the parent ClassLoader first. A pattern is a simple prefix that is checked against"
                                    + " the fully qualified class name. This setting should generally not be modified. To add another pattern we"
                                    + " recommend to use \"classloader.parent-first-patterns.additional\" instead.");

    @Documentation.Section(Documentation.Sections.EXPERT_CLASS_LOADING)
    public static final ConfigOption<List<String>> ALWAYS_PARENT_FIRST_LOADER_PATTERNS_ADDITIONAL =
            ConfigOptions.key("classloader.parent-first-patterns.additional")
                    .stringType()
                    .asList()
                    .defaultValues()
                    .withDescription(
                            "A (semicolon-separated) list of patterns that specifies which classes should always be"
                                    + " resolved through the parent ClassLoader first. A pattern is a simple prefix that is checked against"
                                    + " the fully qualified class name. These patterns are appended to \""
                                    + ALWAYS_PARENT_FIRST_LOADER_PATTERNS.key()
                                    + "\".");

    @Documentation.Section(Documentation.Sections.EXPERT_CLASS_LOADING)
    public static final ConfigOption<Boolean> FAIL_ON_USER_CLASS_LOADING_METASPACE_OOM =
            ConfigOptions.key("classloader.fail-on-metaspace-oom-error")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription(
                            "Fail Flink JVM processes if 'OutOfMemoryError: Metaspace' is "
                                    + "thrown while trying to load a user code class.");

    public static String[] getParentFirstLoaderPatterns(ReadableConfig config) {
        List<String> base = config.get(ALWAYS_PARENT_FIRST_LOADER_PATTERNS);
        List<String> append = config.get(ALWAYS_PARENT_FIRST_LOADER_PATTERNS_ADDITIONAL);
        return mergeListsToArray(base, append);
    }

    @Documentation.Section(Documentation.Sections.EXPERT_CLASS_LOADING)
    public static final ConfigOption<Boolean> CHECK_LEAKED_CLASSLOADER =
            ConfigOptions.key("classloader.check-leaked-classloader")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription(
                            "Fails attempts at loading classes if the user classloader of a job is used after it has "
                                    + "terminated.\n"
                                    + "This is usually caused by the classloader being leaked by lingering threads or misbehaving libraries, "
                                    + "which may also result in the classloader being used by other jobs.\n"
                                    + "This check should only be disabled if such a leak prevents further jobs from running.");

    /**
     * Plugin-specific option of {@link #ALWAYS_PARENT_FIRST_LOADER_PATTERNS}. Plugins use this
     * parent first list instead of the global version.
     */
    @Documentation.ExcludeFromDocumentation(
            "Plugin classloader list is considered an implementation detail. "
                    + "Configuration only included in case to mitigate unintended side-effects of this young feature.")
    public static final ConfigOption<List<String>> PLUGIN_ALWAYS_PARENT_FIRST_LOADER_PATTERNS =
            ConfigOptions.key("plugin.classloader.parent-first-patterns.default")
                    .stringType()
                    .asList()
                    .defaultValues(
                            ArrayUtils.concat(
                                    new String[] {
                                        "java.", "org.apache.flink.", "javax.annotation."
                                    },
                                    PARENT_FIRST_LOGGING_PATTERNS))
                    .withDescription(
                            "A (semicolon-separated) list of patterns that specifies which classes should always be"
                                    + " resolved through the plugin parent ClassLoader first. A pattern is a simple prefix that is checked "
                                    + " against the fully qualified class name. This setting should generally not be modified. To add another "
                                    + " pattern we recommend to use \"plugin.classloader.parent-first-patterns.additional\" instead.");

    @Documentation.ExcludeFromDocumentation(
            "Plugin classloader list is considered an implementation detail. "
                    + "Configuration only included in case to mitigate unintended side-effects of this young feature.")
    public static final ConfigOption<List<String>>
            PLUGIN_ALWAYS_PARENT_FIRST_LOADER_PATTERNS_ADDITIONAL =
                    ConfigOptions.key("plugin.classloader.parent-first-patterns.additional")
                            .stringType()
                            .asList()
                            .defaultValues()
                            .withDescription(
                                    "A (semicolon-separated) list of patterns that specifies which classes should always be"
                                            + " resolved through the plugin parent ClassLoader first. A pattern is a simple prefix that is checked "
                                            + " against the fully qualified class name. These patterns are appended to \""
                                            + PLUGIN_ALWAYS_PARENT_FIRST_LOADER_PATTERNS.key()
                                            + "\".");

    public static String[] getPluginParentFirstLoaderPatterns(Configuration config) {
        List<String> base = config.get(PLUGIN_ALWAYS_PARENT_FIRST_LOADER_PATTERNS);
        List<String> append = config.get(PLUGIN_ALWAYS_PARENT_FIRST_LOADER_PATTERNS_ADDITIONAL);
        return mergeListsToArray(base, append);
    }

    @Internal
    public static String[] mergeListsToArray(List<String> base, List<String> append) {
        return Iterables.toArray(Iterables.concat(base, append), String.class);
    }

    // ------------------------------------------------------------------------
    //  process parameters
    // ------------------------------------------------------------------------

    public static final ConfigOption<String> FLINK_JAVA_HOME =
            ConfigOptions.key("env.java.home")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            Description.builder()
                                    .text(
                                            "Location where Java is installed. If not specified,"
                                                    + " Flink will use your default Java installation.")
                                    .build());

    public static final ConfigOption<String> FLINK_JVM_OPTIONS =
            ConfigOptions.key("env.java.opts.all")
                    .stringType()
                    .defaultValue("")
                    .withDeprecatedKeys("env.java.opts")
                    .withDescription(
                            Description.builder()
                                    .text(
                                            "Java options to start the JVM of all Flink processes with.")
                                    .build());

    public static final ConfigOption<String> FLINK_JM_JVM_OPTIONS =
            ConfigOptions.key("env.java.opts.jobmanager")
                    .stringType()
                    .defaultValue("")
                    .withDescription(
                            Description.builder()
                                    .text("Java options to start the JVM of the JobManager with.")
                                    .build());

    public static final ConfigOption<String> FLINK_TM_JVM_OPTIONS =
            ConfigOptions.key("env.java.opts.taskmanager")
                    .stringType()
                    .defaultValue("")
                    .withDescription(
                            Description.builder()
                                    .text("Java options to start the JVM of the TaskManager with.")
                                    .build());

    public static final ConfigOption<String> FLINK_HS_JVM_OPTIONS =
            ConfigOptions.key("env.java.opts.historyserver")
                    .stringType()
                    .defaultValue("")
                    .withDescription(
                            Description.builder()
                                    .text(
                                            "Java options to start the JVM of the HistoryServer with.")
                                    .build());

    public static final ConfigOption<String> FLINK_CLI_JVM_OPTIONS =
            ConfigOptions.key("env.java.opts.client")
                    .stringType()
                    .defaultValue("")
                    .withDescription(
                            Description.builder()
                                    .text("Java options to start the JVM of the Flink Client with.")
                                    .build());

    public static final ConfigOption<String> FLINK_SQL_GATEWAY_JVM_OPTIONS =
            ConfigOptions.key("env.java.opts.sql-gateway")
                    .stringType()
                    .defaultValue("")
                    .withDescription(
                            Description.builder()
                                    .text(
                                            "Java options to start the JVM of the Flink SQL Gateway with.")
                                    .build());

    public static final ConfigOption<String> FLINK_DEFAULT_JVM_OPTIONS =
            ConfigOptions.key("env.java.default-opts.all")
                    .stringType()
                    .defaultValue("")
                    .withDescription(
                            Description.builder()
                                    .text(
                                            "A string of default JVM options to prepend to %s."
                                                    + " This is intended to be set by administrators.",
                                            code(FLINK_JVM_OPTIONS.key()))
                                    .build());

    public static final ConfigOption<String> FLINK_DEFAULT_JM_JVM_OPTIONS =
            ConfigOptions.key("env.java.default-opts.jobmanager")
                    .stringType()
                    .defaultValue("")
                    .withDescription(
                            Description.builder()
                                    .text(
                                            "A string of default JVM options to prepend to %s."
                                                    + " This is intended to be set by administrators.",
                                            code(FLINK_JM_JVM_OPTIONS.key()))
                                    .build());

    public static final ConfigOption<String> FLINK_DEFAULT_TM_JVM_OPTIONS =
            ConfigOptions.key("env.java.default-opts.taskmanager")
                    .stringType()
                    .defaultValue("")
                    .withDescription(
                            Description.builder()
                                    .text(
                                            "A string of default JVM options to prepend to %s."
                                                    + " This is intended to be set by administrators.",
                                            code(FLINK_TM_JVM_OPTIONS.key()))
                                    .build());

    /**
     * This option is here only for documentation generation, it is only evaluated in the shell
     * scripts.
     */
    @SuppressWarnings("unused")
    public static final ConfigOption<String> FLINK_LOG_DIR =
            ConfigOptions.key("env.log.dir")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            "Defines the directory where the Flink logs are saved. It has to be an absolute path."
                                    + " (Defaults to the log directory under Flink’s home)");

    /**
     * The config parameter defining the directory for Flink PID file. see: {@code
     * bin/config.sh#KEY_ENV_PID_DIR} and {@code bin/config.sh#DEFAULT_ENV_PID_DIR}
     */
    public static final ConfigOption<String> FLINK_PID_DIR =
            ConfigOptions.key("env.pid.dir")
                    .stringType()
                    .defaultValue("/tmp")
                    .withDescription(
                            "Defines the directory where the flink-<host>-<process>.pid files are saved.");

    /**
     * This option is here only for documentation generation, it is only evaluated in the shell
     * scripts.
     */
    @SuppressWarnings("unused")
    public static final ConfigOption<Integer> FLINK_LOG_MAX =
            ConfigOptions.key("env.log.max")
                    .intType()
                    .defaultValue(10)
                    .withDescription("The maximum number of old log files to keep.");

    /**
     * This option is here only for documentation generation, it is only evaluated in the shell
     * scripts.
     */
    @SuppressWarnings("unused")
    public static final ConfigOption<String> FLINK_LOG_LEVEL =
            ConfigOptions.key("env.log.level")
                    .stringType()
                    .defaultValue("INFO")
                    .withDescription("Defines the level of the root logger.");

    /**
     * This option is here only for documentation generation, it is only evaluated in the shell
     * scripts.
     */
    @SuppressWarnings("unused")
    public static final ConfigOption<Boolean> FLINK_STD_REDIRECT_TO_FILE =
            ConfigOptions.key("env.stdout-err.redirect-to-file")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription(
                            "Whether redirect stdout and stderr to files when running foreground. "
                                    + "If enabled, logs won't append the console too. "
                                    + "Note that redirected files do not support rolling rotate.");

    /**
     * This option is here only for documentation generation, it is only evaluated in the shell
     * scripts.
     */
    @SuppressWarnings("unused")
    public static final ConfigOption<String> FLINK_SSH_OPTIONS =
            ConfigOptions.key("env.ssh.opts")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            "Additional command line options passed to SSH clients when starting or stopping JobManager,"
                                    + " TaskManager, and Zookeeper services (start-cluster.sh, stop-cluster.sh, start-zookeeper-quorum.sh,"
                                    + " stop-zookeeper-quorum.sh).");

    /**
     * This option is here only for documentation generation, it is only evaluated in the shell
     * scripts.
     */
    @SuppressWarnings("unused")
    public static final ConfigOption<String> FLINK_HADOOP_CONF_DIR =
            ConfigOptions.key("env.hadoop.conf.dir")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            "Path to hadoop configuration directory. It is required to read HDFS and/or YARN"
                                    + " configuration. You can also set it via environment variable.");

    /**
     * This option is here only for documentation generation, it is only evaluated in the shell
     * scripts.
     */
    @SuppressWarnings("unused")
    public static final ConfigOption<String> FLINK_YARN_CONF_DIR =
            ConfigOptions.key("env.yarn.conf.dir")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            "Path to yarn configuration directory. It is required to run flink on YARN. You can also"
                                    + " set it via environment variable.");

    /**
     * This option is here only for documentation generation, it is only evaluated in the shell
     * scripts.
     */
    @SuppressWarnings("unused")
    public static final ConfigOption<String> FLINK_HBASE_CONF_DIR =
            ConfigOptions.key("env.hbase.conf.dir")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            "Path to hbase configuration directory. It is required to read HBASE configuration."
                                    + " You can also set it via environment variable.");

    // ------------------------------------------------------------------------
    //  generic io
    // ------------------------------------------------------------------------

    /**
     * The config parameter defining the directories for temporary files, separated by ",", "|", or
     * the system's {@link java.io.File#pathSeparator}.
     */
    @Documentation.OverrideDefault(
            "'LOCAL_DIRS' on Yarn. System.getProperty(\"java.io.tmpdir\") in standalone.")
    @Documentation.Section(Documentation.Sections.COMMON_MISCELLANEOUS)
    public static final ConfigOption<String> TMP_DIRS =
            key("io.tmp.dirs")
                    .stringType()
                    .defaultValue(System.getProperty("java.io.tmpdir"))
                    .withDeprecatedKeys("taskmanager.tmp.dirs")
                    .withDescription(
                            "Directories for temporary files, separated by\",\", \"|\", or the system's java.io.File.pathSeparator.");

    // ------------------------------------------------------------------------
    //  program
    // ------------------------------------------------------------------------

    public static final ConfigOption<Integer> DEFAULT_PARALLELISM =
            ConfigOptions.key("parallelism.default")
                    .intType()
                    .defaultValue(1)
                    .withDescription("Default parallelism for jobs.");

    // ------------------------------------------------------------------------
    //  file systems
    // ------------------------------------------------------------------------

    /** The default filesystem scheme, used for paths that do not declare a scheme explicitly. */
    @Documentation.Section(Documentation.Sections.COMMON_MISCELLANEOUS)
    public static final ConfigOption<String> DEFAULT_FILESYSTEM_SCHEME =
            ConfigOptions.key("fs.default-scheme")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            "The default filesystem scheme, used for paths that do not declare a scheme explicitly."
                                    + " May contain an authority, e.g. host:port in case of an HDFS NameNode.");

    @Documentation.Section(Documentation.Sections.COMMON_MISCELLANEOUS)
    public static final ConfigOption<String> ALLOWED_FALLBACK_FILESYSTEMS =
            ConfigOptions.key("fs.allowed-fallback-filesystems")
                    .stringType()
                    .defaultValue("")
                    .withDescription(
                            "A (semicolon-separated) list of file schemes, for which Hadoop can be used instead "
                                    + "of an appropriate Flink plugin. (example: s3;wasb)");

    /** Specifies whether file output writers should overwrite existing files by default. */
    @Documentation.Section(Documentation.Sections.DEPRECATED_FILE_SINKS)
    public static final ConfigOption<Boolean> FILESYTEM_DEFAULT_OVERRIDE =
            key("fs.overwrite-files")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription(
                            "Specifies whether file output writers should overwrite existing files by default. Set to"
                                    + " \"true\" to overwrite by default,\"false\" otherwise.");

    /**
     * Specifies whether the file systems should always create a directory for the output, even with
     * a parallelism of one.
     */
    @Documentation.Section(Documentation.Sections.DEPRECATED_FILE_SINKS)
    public static final ConfigOption<Boolean> FILESYSTEM_OUTPUT_ALWAYS_CREATE_DIRECTORY =
            key("fs.output.always-create-directory")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription(
                            "File writers running with a parallelism larger than one create a directory for the output"
                                    + " file path and put the different result files (one per parallel writer task) into that directory."
                                    + " If this option is set to \"true\", writers with a parallelism of 1 will also create a"
                                    + " directory and place a single result file into it. If the option is set to \"false\","
                                    + " the writer will directly create the file directly at the output path, without creating a containing"
                                    + " directory.");

    /**
     * The total number of input plus output connections that a file system for the given scheme may
     * open. Unlimited be default.
     */
    public static ConfigOption<Integer> fileSystemConnectionLimit(String scheme) {
        return ConfigOptions.key("fs." + scheme + ".limit.total").intType().defaultValue(-1);
    }

    /**
     * The total number of input connections that a file system for the given scheme may open.
     * Unlimited be default.
     */
    public static ConfigOption<Integer> fileSystemConnectionLimitIn(String scheme) {
        return ConfigOptions.key("fs." + scheme + ".limit.input").intType().defaultValue(-1);
    }

    /**
     * The total number of output connections that a file system for the given scheme may open.
     * Unlimited be default.
     */
    public static ConfigOption<Integer> fileSystemConnectionLimitOut(String scheme) {
        return ConfigOptions.key("fs." + scheme + ".limit.output").intType().defaultValue(-1);
    }

    /**
     * If any connection limit is configured, this option can be optionally set to define after
     * which time (in milliseconds) stream opening fails with a timeout exception, if no stream
     * connection becomes available. Unlimited timeout be default.
     */
    public static ConfigOption<Long> fileSystemConnectionLimitTimeout(String scheme) {
        return ConfigOptions.key("fs." + scheme + ".limit.timeout").longType().defaultValue(0L);
    }

    /**
     * If any connection limit is configured, this option can be optionally set to define after
     * which time (in milliseconds) inactive streams are reclaimed. This option can help to prevent
     * that inactive streams make up the full pool of limited connections, and no further
     * connections can be established. Unlimited timeout be default.
     */
    public static ConfigOption<Long> fileSystemConnectionLimitStreamInactivityTimeout(
            String scheme) {
        return ConfigOptions.key("fs." + scheme + ".limit.stream-timeout")
                .longType()
                .defaultValue(0L);
    }
}
