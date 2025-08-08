/**
 * MainActivity：Compose UI をセットし、チャット画面を起動します。
 */
package com.negi.pipechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import android.util.Log
import com.negi.pipechat.ui.theme.MediaPipeChatLLMTheme

/**
 * アンケート回答を検証するチャット画面。
 *
 * @param initialQuestion 初期に表示する質問文
 */
@Composable
fun ChatSurveyScreen(
    initialQuestion: String
) {
    // 現在表示中の質問（フォローアップ用に更新）
    var currentQuestion by remember { mutableStateOf(initialQuestion) }
    val context = LocalContext.current
    // LLM マネージャーを Compose のライフサイクルに紐付けて管理
    val validateManager = remember { LLMManager(context) }

    // ユーザー入力テキスト
    var inputText by remember { mutableStateOf("") }
    // チャット履歴: Pair<メッセージ, isUser>
    var chatHistory by remember {
        mutableStateOf(
            listOf(
                "Q: $initialQuestion" to false
            )
        )
    }
    // バリデーション中フラグ
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Compose が破棄される際にマネージャーをクローズ
    DisposableEffect(Unit) {
        onDispose { validateManager.close() }
    }
    // 新しいメッセージ追加時にスクロール
    LaunchedEffect(chatHistory.size) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.lastIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // チャット履歴リスト
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(chatHistory) { (msg, isUser) ->
                ChatBubble(content = msg, isUser = isUser)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        // 入力フォーム & 送信ボタン
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()            // ソフトキーボード追従
                .navigationBarsPadding() // ナビゲーションバー追従
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            /**
             * ユーザーの回答を送信し、LLM で検証を行う。
             * 入力が空欄または処理中の場合は無視。
             */
            fun sendAnswer() {
                if (inputText.isBlank() || isLoading) return

                // ユーザー回答メッセージを追加
                chatHistory = chatHistory + ("A: $inputText" to true)
                isLoading = true
                keyboardController?.hide() // キーボードを閉じる

                performValidation(
                    question = currentQuestion,
                    answer = inputText,
                    manager = validateManager
                ) { valid, comment, followUp ->
                    // ログ出力（デバッグ用）
                    Log.d("ChatSurvey", "Validation result: valid=$valid, comment='$comment', followUp='$followUp'")

                    // 検証結果メッセージ
                    val resultText = if (valid) "✅ 適切な回答です" else "❌ $comment"
                    chatHistory = chatHistory + (resultText to false)

                    // フォローアップ質問がある場合は次の質問として追加
                    if (!valid && followUp.isNotBlank()) {
                        currentQuestion = followUp
                        chatHistory = chatHistory + ("Q: $followUp" to false)
                    }
                    isLoading = false
                }
                inputText = "" // 入力欄クリア
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("回答を入力…") },
                singleLine = true,
                enabled = !isLoading,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendAnswer() })
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { sendAnswer() },
                enabled = !isLoading
            ) {
                Text(text = if (isLoading) "処理中…" else "送信")
            }
        }
    }
}

/**
 * 回答を LLM に投げて検証し、コールバックで結果を返却します。
 *
 * @param question 検証対象の質問文
 * @param answer   検証対象の回答文
 * @param manager  LLM 生成を管理するインスタンス
 * @param onResult (valid: Boolean, comment: String, followUp: String) -> Unit 検証結果コールバック
 */
private fun performValidation(
    question: String,
    answer: String,
    manager: LLMManager,
    onResult: (valid: Boolean, comment: String, followUp: String) -> Unit
) {
    if (answer.isBlank()) return

    val prompt = """
You are a highly skilled survey analyst. Please evaluate the respondent’s free-text answer based on the following criteria:

1. Relevance — Does it directly respond to the survey question?
2. Specificity — Does it mention concrete issues, examples, or details?
3. Actionability — Is there enough context to understand or take action?

Your response must be **exactly three lines**, using this format:

VALIDATION: Yes or No  
COMMENT: (If No, provide a constructive reason and suggest a better way to answer. If Yes, give a brief positive remark.)  
FOLLOW_UP_QUESTION: (A concise and relevant follow-up question that encourages deeper insight)

Survey Question: $question  
Answer: $answer
""".trimIndent()

    manager.generateResponseAsync(prompt) { raw ->
        val lines = raw.lines()
        val valid = lines.firstOrNull { it.startsWith("VALIDATION:") }
            ?.substringAfter("VALIDATION:")
            ?.trim() == "Yes"
        val comment = lines.firstOrNull { it.startsWith("COMMENT:") }
            ?.substringAfter("COMMENT:")
            ?.trim().orEmpty()
        val followUp = lines.firstOrNull { it.startsWith("FOLLOW_UP_QUESTION:") }
            ?.substringAfter("FOLLOW_UP_QUESTION:")
            ?.trim().orEmpty()
        onResult(valid, comment, followUp)
    }
}

/**
 * メッセージと質問／回答をバブル状に表示するコンポーザブル。
 *
 * @param content 表示するテキスト（"Q:" や "A:" を含む）
 * @param isUser  ユーザー発言かどうか
 */
@Composable
fun ChatBubble(
    content: String,
    isUser: Boolean
) {
    val background = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
    val parts = content.split("\n")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = background,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                parts.forEach { line ->
                    val isQuestion = line.startsWith("Q:")
                    Text(
                        text = line,
                        color = contentColor,
                        fontWeight = if (isQuestion) FontWeight.Bold else FontWeight.Normal,
                        style = if (isQuestion) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = if (isQuestion) 4.dp else 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * メインアクティビティ：Compose のテーマを設定し、
 * ChatSurveyScreen を起動します。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // システムウィンドウとの重なりを防止
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MediaPipeChatLLMTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChatSurveyScreen(
                        initialQuestion = "What problems have you encountered during seed germination after sowing, or in the first few weeks of early growth?"
                    )
                }
            }
        }
    }
}
