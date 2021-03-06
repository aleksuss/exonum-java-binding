#!/usr/bin/env bash
# Runs all tests.
#
# A JVM will be selected by JAVA_HOME environment variable, or, if it is not set,
# inferred from the java executable available on the path.

# Fail immediately in case of errors and/or unset variables
set -eu -o pipefail

# Echo commands so that the progress can be seen in CI server logs.
set -x

# Run Rust and Shell code checks if CHECK_RUST is true, or Java tests if it's not
if [ "$CHECK_RUST" = true ]
then
    # Check the shell scripts
    shellcheck "${TRAVIS_BUILD_DIR}"/exonum-java-binding/tests_profile
    shellcheck "${TRAVIS_BUILD_DIR}"/exonum-java-binding/core/rust/exonum-java/exonum-java
    find "${TRAVIS_BUILD_DIR}" -iname '*.sh' -exec shellcheck -x {} +

    # Install clippy and rustfmt.
    rustup component add clippy
    rustup component add rustfmt
    rustfmt -V
    cargo clippy -V

    echo 'Performing checks over the rust code'
    EJB_RUST_BUILD_DIR="${TRAVIS_BUILD_DIR}/exonum-java-binding/core/rust/"
    cd "${EJB_RUST_BUILD_DIR}"

    # Check the formatting.
    cargo fmt -- --check

    # Run clippy static analysis.
    cargo clippy --all --tests --all-features -- -D warnings

    echo 'Rust checks are completed.'
else
    cd "${TRAVIS_BUILD_DIR}"

    # Set CI Maven arguments. They set batch mode (less verbose Maven output);
    # and enable parallel builds, and parallel tests (Linux only).
    # TODO: Enable on macos when it uses newer JDK that does not hang up when
    #   parallel tests are enabled.
    MAVEN_CONFIG="--batch-mode"
    if [[ ${TRAVIS_OS_NAME} == "linux" ]]; then
      MAVEN_CONFIG="${MAVEN_CONFIG} \
--threads 1C \
-Djunit.jupiter.execution.parallel.enabled=true \
-Djunit.jupiter.execution.parallel.mode.default=concurrent"
    fi
    echo "${MAVEN_CONFIG}" > .mvn/maven.config

    # Run all tests
    ./run_all_tests.sh

    # Verify the scripts generated by service archetype. shellcheck is not available on macOS.
    if [[ "${TRAVIS_OS_NAME}" == "linux" ]]; then
      shellcheck exonum-java-binding/service-archetype/target/test-classes/projects/project/integration-test/start-testnet.sh
    fi

    # Upload the coverage report to Coveralls from a single job only
    if [[ "${TRAVIS_JOB_NAME}" == "Linux / JDK 14 / CHECK_RUST=false" ]]; then
      mvn org.eluder.coveralls:coveralls-maven-plugin:report
    fi
fi
