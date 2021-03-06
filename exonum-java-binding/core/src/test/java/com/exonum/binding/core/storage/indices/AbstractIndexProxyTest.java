/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.core.storage.indices;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.exonum.binding.core.proxy.Cleaner;
import com.exonum.binding.core.proxy.NativeHandle;
import com.exonum.binding.core.storage.database.AbstractAccess;
import com.exonum.binding.core.storage.database.Fork;
import com.exonum.binding.core.storage.database.Snapshot;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class AbstractIndexProxyTest {

  private static final String INDEX_NAME = "index_name";

  private AbstractIndexProxy proxy;

  @Test
  void testConstructor() {
    AbstractAccess access = createFork();
    proxy = new IndexProxyImpl(access);

    assertThat(proxy.dbAccess, equalTo(access));
  }

  @Test
  void constructorFailsIfNullView() {
    AbstractAccess dbAccess = null;

    assertThrows(NullPointerException.class, () -> proxy = new IndexProxyImpl(dbAccess));
  }

  @Test
  void notifyModifiedThrowsIfSnapshotPassed() {
    Snapshot dbView = createSnapshot();

    proxy = new IndexProxyImpl(dbView);

    UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class,
        () -> proxy.notifyModified());

    Pattern pattern = Pattern.compile("Cannot modify the access: .*[Ss]napshot.*"
        + "\\nUse a Fork to modify any collection\\.", Pattern.MULTILINE);
    assertThat(thrown, hasMessage(matchesPattern(pattern)));
  }

  @Test
  void notifyModifiedAcceptsFork() {
    Fork dbView = createFork();

    proxy = new IndexProxyImpl(dbView);

    assertThatCode(proxy::notifyModified).doesNotThrowAnyException();
  }

  @Test
  void name() {
    Snapshot dbView = createSnapshot();
    proxy = new IndexProxyImpl(dbView);

    assertThat(proxy.getName(), equalTo(INDEX_NAME));
  }

  /**
   * Create a non-owning fork.
   */
  private Fork createFork() {
    return Fork.newInstance(0x01, false, new Cleaner());
  }

  /**
   * Create a non-owning snapshot.
   */
  private Snapshot createSnapshot() {
    return Snapshot.newInstance(0x02, false, new Cleaner());
  }

  private static class IndexProxyImpl extends AbstractIndexProxy {

    private static final long NATIVE_HANDLE = 0x11L;

    IndexProxyImpl(AbstractAccess access) {
      super(new NativeHandle(NATIVE_HANDLE), IndexAddress.valueOf(INDEX_NAME), access);
    }
  }

}
