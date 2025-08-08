# MediaPipeChat Survey UI

このリポジトリは、Compose ベースのチャットインターフェースを使用して、アンケート回答を LLM（大規模言語モデル）で検証する Android アプリケーションのサンプル実装です。

---

## 主要機能

* **チャット形式のアンケート画面**: 動的に質問と回答をバブル表示します。
* **LLM 検証エンジン連携**: 回答の「関連性」「具体性」「詳細度」を判定し、フォローアップ質問を自動生成。
* **自動スクロール**: 新しいメッセージが追加されると最新行までスクロール。
* **キーボード・ナビゲーションバー追従**: 入力フォームが常に見えるようにソフトキーボードやナビゲーションバーに合わせてレイアウトを調整。

## プロジェクト構成

```
├── app
│   └── src/main/java/com/negi/pipechat
│       ├── MainActivity.kt         # アプリのエントリーポイント
│       ├── ChatSurveyScreen.kt     # Compose ベースのチャット画面実装
│       └── LLMManager.kt           # LLM コールを非同期で処理するユーティリティ
└── ui
    └── theme
        └── MediaPipeChatLLMTheme.kt # Material3 テーマ定義
```

## セットアップ手順

1. Android Studio Arctic Fox 以上をインストール
2. JDK 11 を設定
3. このリポジトリをクローン:

   ```bash
   git clone https://github.com/ishizuki-tech/MediaPipeChat.git
   ```
4. `app/build.gradle` の `applicationId` を適切に変更
5. LLM API キーを環境変数または `local.properties` に設定
6. プロジェクトを同期し、エミュレータまたは実機でビルド

## 主要コンポーネント解説

### MainActivity

* アプリ起動時に `ChatSurveyScreen` を呼び出します。
* `MediaPipeChatLLMTheme` で Material3 を適用します。

### ChatSurveyScreen

* `initialQuestion` を受け取り、最初の質問バブルを生成します。
* `chatHistory` に質問・回答ペアを保持し、`LazyColumn` で表示します。
* 入力フォームから回答を受け取り、送信ボタンまたは IME の送信アクションで `performValidation` を呼び出します。
* バリデーション結果を受け取り、正誤メッセージやフォローアップ質問をチャット履歴に反映します。
* `DisposableEffect` で LLM マネージャーのクローズ処理を行います。
* `LaunchedEffect` で新規メッセージ時にリストを自動スクロールします。

## ロジックフロー

以下のシーケンスで処理が進みます：

1. **画面初期化**: `ChatSurveyScreen` が `initialQuestion` を履歴に追加し、最初の質問バブルを描画。
2. **ユーザー入力**: `OutlinedTextField` と「送信」ボタン、または IME の送信操作で `sendAnswer()` を呼び出し。
3. **送信処理**:

   * 入力が空または処理中なら無視。
   * ユーザー回答をチャット履歴に追加し、`isLoading = true` に設定。
   * キーボードを閉じる。
4. **LLM バリデーション**: `performValidation` を呼び出し、以下を非同期で実行：

   1. 質問と回答を組み込んだプロンプトを生成。
   2. `LLMManager` 経由で LLM にプロンプトを送信。
   3. レスポンスを3行形式で受信。
   4. 各行をパースして `valid`, `comment`, `followUp` を算出。
5. **結果反映**:

   * 「✅ 適切な回答です」または「❌ コメント」をチャット履歴に追加。
   * `valid == false` かつ `followUp` が空でない場合は新質問として履歴に追加。
   * `isLoading = false` に戻し、入力欄をクリア。
6. **UI 更新**: `LaunchedEffect` により最新メッセージ位置まで自動スクロール。

## LLM プロンプト詳細

```plaintext
You are an expert survey analyst. Evaluate the following respondent’s free-text answer against these criteria:
1. Relevance: Does it directly address the question?
2. Specificity: Does it name concrete issues or examples?
3. Detail: Is there enough context to be actionable?

Respond in exactly three lines using this format:
VALIDATION: Yes or No
COMMENT: (If No, suggest a follow-up question; if Yes, write a brief positive remark)
FOLLOW_UP_QUESTION: (One concise follow-up question)

Survey Question: $question
Answer: $answer
```

* **VALIDATION**: 回答の妥当性 (Yes/No)
* **COMMENT**: No の場合はフォローアップ提案、Yes の場合は短い肯定コメント
* **FOLLOW\_UP\_QUESTION**: 必要に応じた次の質問

## カスタマイズポイント

* **プロンプト調整**: `performValidation` 内のプロンプト文を変更して評価軸を追加・変更可能。
* **レスポンスパーサー**: 行解析ロジックを抽象化してテスト可能に分離。

## テスト

* `LLMManager` のモックを使ったレスポンス解析テスト
* `ChatSurveyScreen` の UI テスト（Compose Testing API）

## モデルダウンロードスクリプト

プロジェクト内の `src/main/assets/models` フォルダにモデルファイルを一括でダウンロードするための Bash スクリプトです。

### スクリプト概要

* ファイル名: `download_models.sh`
* デフォルト動作: `gemma3-1b-it-int8.task` をダウンロード
* 指定したファイルが存在しない場合にのみダウンロード
* `HF_TOKEN` 環境変数で Hugging Face のアクセストークンを指定する必要があります。

### 使い方

```bash
# スクリプトに実行権限を付与
chmod +x download_models.sh

# デフォルトモデルをダウンロード
./download_models.sh

# 複数ファイルを指定してダウンロード
./download_models.sh gemma3-1b-it-int4.task gemma3-1b-it-int16.task
```

#### 環境変数

* `HF_TOKEN`: Hugging Face API トークン。プライベートリポジトリから取得する場合に必須。

  ```bash
  export HF_TOKEN="your_hf_token_here"
  ```

#### スクリプトのロジック

1. 環境変数 `HF_TOKEN` が未設定の場合はエラー終了。
2. ダウンロード対象ファイルのリスト (`MODEL_FILES`) を引数またはデフォルトで決定。
3. `src/main/assets/models` フォルダを作成し、移動。
4. 各ファイルが存在しなければ、`curl` or `wget` でダウンロード。
5. ダウンロード成功を検証し、状況をログ出力。

## ライセンス

MIT License
