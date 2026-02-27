# ヒューリスティックコンテストライブラリ (Heuristics Contest Library)

このプロジェクトは、ヒューリスティックコンテスト（AtCoder Heuristic Contestなど）への参加を支援するために設計されたライブラリおよびツールセットです。解答プログラムのシミュレーションをローカルまたは分散環境で実行し、その結果を評価・分析する機能を提供します。

## 機能

- **MarathonCodeSimulator**: 解答コードを複数のシードに対して実行するツールです。
    - 並列実行をサポートしています。
    - 実行ログと標準出力を AWS S3 にアップロードできます。
    - 結果を AWS DynamoDB に保存できます。
- **MarathonCodeEvaluator**: シミュレーション結果を集計・分析するツールです。
    - ローカルファイル、S3、または DynamoDB からデータを取得します。
    - 分析のために出力をグループ化およびフォーマットします。

## 前提条件

- Java 8 以上
- Gradle（ラッパーが含まれています）
- AWS クレデンシャル（S3 や DynamoDB 機能を使用する場合のみ必要）が `~/.aws/config` と `~/.aws/credentials` に設定されていること。

## ビルド

プロジェクトをビルドし、実行可能な JAR ファイルを生成するには、以下のコマンドを実行します：

```bash
./gradlew jar
```

JAR ファイルは `build/libs/heuristics-contest-library-1.0-SNAPSHOT.jar` に生成されます。

## 使用方法

### MarathonCodeSimulator

指定されたシード範囲でシミュレーションを実行します。

**基本的な使用例（ローカル）:**

```bash
java -cp build/libs/heuristics-contest-library-1.0-SNAPSHOT.jar hiro116s.simulator.MarathonCodeSimulator \
  --commandTemplate 'java Main $SEED' \
  --minSeed 1 \
  --maxSeed 50 \
  --numThreads 4
```

**AWS S3 と DynamoDB を使用する例:**

```bash
java -cp build/libs/heuristics-contest-library-1.0-SNAPSHOT.jar hiro116s.simulator.MarathonCodeSimulator \
  --commandTemplate 'java Main $SEED' \
  --minSeed 1 \
  --maxSeed 100 \
  --contestName my-contest \
  --s3 --s3Bucket my-s3-bucket \
  --dynamo PRODUCTION
```

#### 引数

| 引数 | 説明 | デフォルト値 |
| --- | --- | --- |
| `--commandTemplate` | **必須**。実行するコマンド。`$SEED` プレースホルダーを含める必要があります。 | |
| `--minSeed` | 開始シード。 | 1 |
| `--maxSeed` | 終了シード。 | 100 |
| `--seeds` | 実行する特定のシードのカンマ区切りリスト（min/max よりも優先されます）。 | |
| `--numThreads` | 並列実行のスレッド数。 | 1 |
| `--timeout` | 各実行のタイムアウト時間（ミリ秒）。 | Long.MAX_VALUE |
| `--contestName` | コンテスト名。S3/DynamoDB オプションを使用する場合に必須です。 | |
| `--s3` | S3 へのログアップロードを有効にします。 | false |
| `--s3Stdout` | S3 への標準出力のアップロードを有効にします。 | false |
| `--s3Bucket` | S3 バケット名。 | |
| `--dynamo` | DynamoDB 更新タイプ: `NONE`, `LOCAL`, `PRODUCTION`。 | NONE |
| `--stdoutDir` | 標準出力を保存するディレクトリ。 | `./stdout` |
| `--stderrDir` | 標準エラー出力を保存するディレクトリ。 | `./error` |
| `--logOutputDir` | ログを保存するディレクトリ。 | `./log` |
| `--debugMode` | デバッグモードを有効にします（すべての標準エラーを出力します）。 | false |

### MarathonCodeEvaluator

シミュレーション結果を評価および集計します。

**使用例（ローカル）:**

```bash
java -cp build/libs/heuristics-contest-library-1.0-SNAPSHOT.jar hiro116s.simulator.MarathonCodeEvaluator \
  --source LOCAL \
  --logInputDir ./log \
  --groupByKeys "param1,param2"
```

**使用例（S3）:**

```bash
java -cp build/libs/heuristics-contest-library-1.0-SNAPSHOT.jar hiro116s.simulator.MarathonCodeEvaluator \
  --source S3 \
  --s3Bucket my-s3-bucket \
  --contestName my-contest
```

#### 引数

| 引数 | 説明 | デフォルト値 |
| --- | --- | --- |
| `--source` | データソース: `LOCAL`, `S3`, `DYNAMO_DB_PROD`, `DYNAMO_DB_LOCAL`。 | LOCAL |
| `--logInputDir` | ログディレクトリへのパス（source が LOCAL の場合に使用）。 | `./src/main/java/log` |
| `--groupByKeys` | 結果をグループ化するためのカンマ区切りのキー。 | |
| `--contestName` | コンテスト名（source が S3/DynamoDB の場合に必須）。 | |
| `--s3Bucket` | S3 バケット名。 | |
| `--s3CacheEnabled` | S3 のキャッシュを有効にします。 | false |
| `--dynamoDbTableName` | DynamoDB テーブル名。 | contest_scores |
