package com.example.jobmatchai

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.jobmatchai.ui.theme.JobMatchAiTheme
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JobMatchAiTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ResumeGeneratorApp(innerPadding)
                }
            }
        }
    }
}

@Composable
fun ResumeGeneratorApp(innerPadding: PaddingValues) {
    val context = LocalContext.current
    var jobDescription by rememberSaveable { mutableStateOf("") }
    var resumeFile by remember { mutableStateOf<File?>(null) }
    var atsScore by rememberSaveable { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            resumeFile = context.getFileFromUri(it)
            if (resumeFile == null) {
                errorMessage = "Failed to upload resume. Please try again."
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(text = "AI Resume & Cover Letter Generator", style = MaterialTheme.typography.headlineSmall)
        }

        item {
            TextField(
                value = jobDescription,
                onValueChange = { jobDescription = it },
                label = { Text("Job Description") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Button(
                onClick = { launcher.launch("application/pdf") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Upload Resume")
            }
        }

        if (errorMessage != null) {
            item {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        }

        item {
            Button(
                onClick = {
                    isLoading = true
                    atsScore = calculateATSScore(resumeFile, jobDescription)
                    isLoading = false
                },
                enabled = !isLoading && resumeFile != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Generate ATS Score")
                }
            }
        }

        item {
            Text(text = "ATS Score: $atsScore", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

// Convert URI to File
fun Context.getFileFromUri(uri: Uri): File? {
    return try {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "uploaded_resume.pdf")
        inputStream?.use { input ->
            FileOutputStream(file).use { output -> input.copyTo(output) }
        }
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Extract text from PDF using pdfbox-android
fun extractTextFromPdf(file: File): String {
    return try {
        PDDocument.load(file).use { document ->
            PDFTextStripper().getText(document)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }
}

// Calculate ATS Score
fun calculateATSScore(resume: File?, jobDescription: String): Int {
    if (resume == null || jobDescription.isBlank()) return 0
    return try {
        val resumeText = extractTextFromPdf(resume)
        val jobKeywords = jobDescription.split(" ").toSet()
        val matchedKeywords = jobKeywords.count { it in resumeText }
        (matchedKeywords * 100 / jobKeywords.size).coerceIn(0, 100)
    } catch (e: Exception) {
        e.printStackTrace()
        0
    }
}

// Preview UI in Android Studio
@Preview(showBackground = true)
@Composable
fun PreviewResumeGeneratorApp() {
    JobMatchAiTheme {
        ResumeGeneratorApp(innerPadding = PaddingValues(0.dp))
    }
}