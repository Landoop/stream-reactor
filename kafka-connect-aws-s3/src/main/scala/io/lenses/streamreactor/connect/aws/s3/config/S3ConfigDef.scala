
/*
 * Copyright 2020 Lenses.io
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

package io.lenses.streamreactor.connect.aws.s3.config

import com.datamountaineer.streamreactor.common.config.base.traits.{BaseConfig, ConnectionSettings, ErrorPolicySettings, KcqlSettings, NumberRetriesSettings, UserSettings}

import java.util
import org.apache.kafka.common.config.ConfigDef
import org.apache.kafka.common.config.ConfigDef.{Importance, Type}

object S3ConfigDef {

  import S3ConfigSettings._

  val config: ConfigDef = new ConfigDef()
    .define(
      AWS_ACCESS_KEY,
      Type.STRING,
      "",
      Importance.HIGH,
      "AWS access key"
    )
    .define(
      AWS_SECRET_KEY,
      Type.PASSWORD,
      "",
      Importance.HIGH,
      "AWS password key"
    )
    .define(
      AUTH_MODE,
      Type.STRING,
      AuthMode.Default.toString,
      Importance.HIGH,
      "Authenticate mode, 'env' or 'default'"
    )
    .define(
      CUSTOM_ENDPOINT,
      Type.STRING,
      "",
      Importance.LOW,
      "Custom S3-compatible endpoint (usually for testing)"
    )
    .define(
      ENABLE_VIRTUAL_HOST_BUCKETS,
      Type.BOOLEAN,
      false,
      Importance.LOW,
      "Enable virtual host buckets"
    )
    .define(KcqlKey, Type.STRING, Importance.HIGH, KCQL_DOC)

}

case class S3ConfigDefBuilder(props: util.Map[String, String])
  extends BaseConfig(S3ConfigSettings.CONNECTOR_PREFIX, S3ConfigDef.config, props)
    with KcqlSettings
    with ErrorPolicySettings
    with NumberRetriesSettings
    with UserSettings
    with ConnectionSettings
