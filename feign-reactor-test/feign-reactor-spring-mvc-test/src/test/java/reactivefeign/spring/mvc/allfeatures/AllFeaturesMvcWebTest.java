/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactivefeign.spring.mvc.allfeatures;

import org.junit.Ignore;
import org.junit.Test;
import reactivefeign.ReactiveFeign;
import reactivefeign.webclient.WebReactiveFeign;

/**
 * @author Sergii Karpenko
 *
 * Tests ReactiveFeign built on Spring Mvc annotations.
 */
public class AllFeaturesMvcWebTest extends AllFeaturesMvcTest{

	@Override
	protected ReactiveFeign.Builder<AllFeaturesMvc> builder() {
		return WebReactiveFeign.builder();
	}

	@Test
	@Override
	@Ignore
	public void shouldReturnFirstResultBeforeSecondSent() {
	}

	@Test
	@Override
	@Ignore
	public void shouldMirrorStringStreamBody() {
	}
}
