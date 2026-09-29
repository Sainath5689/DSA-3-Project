/* =========================================
   NEAR-DUPLICATE QUESTION FINDER
   FRONTEND LOGIC
========================================= */


/* =========================================
   GLOBAL DATA
========================================= */

let comparisonHistory =
    JSON.parse(
        localStorage.getItem("comparisonHistory") || "[]"
    );

let batchResults = [];

let selectedCSV = null;


/* =========================================
   DOM HELPERS
========================================= */

const $ = (selector) =>
    document.querySelector(selector);

const $$ = (selector) =>
    document.querySelectorAll(selector);


/* =========================================
   NAVIGATION
========================================= */

const pageTitles = {

    dashboard: [
        "Dashboard",
        "Analyze and detect near-duplicate questions."
    ],

    compare: [
        "Compare Questions",
        "Measure the similarity between two questions."
    ],

    batch: [
        "Batch Analysis",
        "Process multiple question pairs from a CSV dataset."
    ],

    method: [
        "Methodology",
        "Understand the algorithms used by the system."
    ],

    about: [
        "About Project",
        "Near-Duplicate Question Finder for Q&A Platforms."
    ]

};


function showSection(sectionId) {

    $$(".page-section").forEach(section => {

        section.classList.remove("active");

    });

    const target =
        document.getElementById(sectionId);

    if (target) {
        target.classList.add("active");
    }


    $$(".nav-item").forEach(item => {

        item.classList.remove("active");

        if (item.dataset.section === sectionId) {
            item.classList.add("active");
        }

    });


    if (pageTitles[sectionId]) {

        $("#pageTitle").textContent =
            pageTitles[sectionId][0];

        $("#pageSubtitle").textContent =
            pageTitles[sectionId][1];

    }


    // Close mobile sidebar
    $(".sidebar").classList.remove("open");

    window.scrollTo({
        top: 0,
        behavior: "smooth"
    });
}


$$(".nav-item").forEach(item => {

    item.addEventListener("click", () => {

        showSection(
            item.dataset.section
        );

    });

});


$$("[data-go]").forEach(button => {

    button.addEventListener("click", () => {

        showSection(
            button.dataset.go
        );

    });

});


/* =========================================
   MOBILE MENU
========================================= */

$("#mobileMenu").addEventListener(
    "click",
    () => {

        $(".sidebar").classList.toggle("open");

    }
);


/* =========================================
   THEME
========================================= */

const savedTheme =
    localStorage.getItem("theme");

if (savedTheme === "dark") {
    document.body.classList.add("dark");
}


$("#themeToggle").addEventListener(
    "click",
    () => {

        document.body.classList.toggle("dark");

        const theme =
            document.body.classList.contains("dark")
                ? "dark"
                : "light";

        localStorage.setItem(
            "theme",
            theme
        );

    }
);


/* =========================================
   TEXT NORMALIZATION
========================================= */

function normalizeText(text) {

    return text
        .toLowerCase()
        .replace(/[^a-z0-9\s]/g, " ")
        .replace(/\s+/g, " ")
        .trim();

}


/* =========================================
   STOP WORDS
========================================= */

const stopWords = new Set([

    "what",
    "is",
    "a",
    "an",
    "the",
    "how",
    "do",
    "i",
    "can",
    "to",
    "of",
    "in",
    "on",
    "for",
    "and",
    "or",
    "explain",
    "give",
    "me",
    "please",
    "with",
    "from",
    "does",
    "it",
    "this",
    "that",
    "be",
    "using"

]);


/* =========================================
   TOKENIZATION
========================================= */

function tokenize(text) {

    const cleaned =
        normalizeText(text);

    if (!cleaned) {
        return [];
    }

    return cleaned
        .split(" ")
        .filter(word =>
            !stopWords.has(word)
        );

}


/* =========================================
   K-SHINGLING
========================================= */

function createShingles(text, k = 3) {

    const words =
        normalizeText(text)
            .split(" ")
            .filter(Boolean);

    const shingles = [];

    if (words.length === 0) {
        return shingles;
    }

    if (words.length < k) {
        shingles.push(
            words.join(" ")
        );

        return shingles;
    }

    for (
        let i = 0;
        i <= words.length - k;
        i++
    ) {

        shingles.push(
            words
                .slice(i, i + k)
                .join(" ")
        );

    }

    return shingles;
}


/* =========================================
   JACCARD SIMILARITY
========================================= */

function jaccardSimilarity(text1, text2) {

    const set1 =
        new Set(tokenize(text1));

    const set2 =
        new Set(tokenize(text2));

    if (
        set1.size === 0 &&
        set2.size === 0
    ) {
        return 0;
    }

    const intersection =
        [...set1].filter(
            word => set2.has(word)
        );

    const union =
        new Set([
            ...set1,
            ...set2
        ]);

    return intersection.length /
        union.size;
}


/* =========================================
   LCS DYNAMIC PROGRAMMING
========================================= */

function lcsSimilarity(text1, text2) {

    const a =
        [...new Set(tokenize(text1))];

    const b =
        [...new Set(tokenize(text2))];

    if (
        a.length === 0 ||
        b.length === 0
    ) {
        return 0;
    }


    const dp =
        Array.from(
            {
                length: a.length + 1
            },
            () =>
                new Array(
                    b.length + 1
                ).fill(0)
        );


    for (
        let i = 1;
        i <= a.length;
        i++
    ) {

        for (
            let j = 1;
            j <= b.length;
            j++
        ) {

            if (
                a[i - 1] ===
                b[j - 1]
            ) {

                dp[i][j] =
                    dp[i - 1][j - 1] + 1;

            } else {

                dp[i][j] =
                    Math.max(
                        dp[i - 1][j],
                        dp[i][j - 1]
                    );

            }

        }

    }


    const lcs =
        dp[a.length][b.length];

    return lcs /
        Math.max(
            a.length,
            b.length
        );
}


/* =========================================
   SIMILARITY ENGINE
========================================= */

function analyzeQuestions(
    question1,
    question2
) {

    const start =
        performance.now();


    const shingle1 =
        createShingles(question1);

    const shingle2 =
        createShingles(question2);


    const shingleSet1 =
        new Set(shingle1);

    const shingleSet2 =
        new Set(shingle2);


    let shingleSimilarity = 0;


    if (
        shingleSet1.size ||
        shingleSet2.size
    ) {

        const intersection =
            [...shingleSet1].filter(
                item =>
                    shingleSet2.has(item)
            );

        const union =
            new Set([
                ...shingleSet1,
                ...shingleSet2
            ]);

        shingleSimilarity =
            intersection.length /
            union.size;
    }


    const jaccard =
        jaccardSimilarity(
            question1,
            question2
        );


    const dpSimilarity =
        lcsSimilarity(
            question1,
            question2
        );


    /*
       Combined score for the frontend.

       Jaccard and DP are combined so that
       the interface gives a smoother score.

       The Java backend can later replace
       this function with the real algorithm.
    */

    const score =
        (
            jaccard * 0.45 +
            dpSimilarity * 0.55
        );


    const elapsed =
        performance.now() - start;


    return {

        score: score,

        shingleSimilarity:
            shingleSimilarity,

        dpSimilarity:
            dpSimilarity,

        processingTime:
            elapsed

    };

}


/* =========================================
   COMPARE QUESTIONS
========================================= */

function compareQuestions(
    question1,
    question2
) {

    if (
        !question1.trim() ||
        !question2.trim()
    ) {

        showToast(
            "Please enter both questions."
        );

        return null;
    }


    return analyzeQuestions(
        question1,
        question2
    );

}


/* =========================================
   DISPLAY RESULT
========================================= */

function displayResult(
    question1,
    question2
) {

    const result =
        compareQuestions(
            question1,
            question2
        );


    if (!result) {
        return;
    }


    const percentage =
        Math.round(
            result.score * 100
        );


    /*
       Frontend demonstration threshold.

       The backend threshold can be changed
       later without changing the UI.
    */

    const isDuplicate =
        result.score >= 0.50;


    const resultPanel =
        $("#resultPanel");

    resultPanel.classList.remove(
        "hidden"
    );


    $("#scoreValue").textContent =
        percentage + "%";


    $("#resultTitle").textContent =
        isDuplicate
            ? "Near Duplicate"
            : "Not a Near Duplicate";


    $("#resultDescription").textContent =
        isDuplicate
            ? "The questions have substantial textual similarity."
            : "The questions do not have enough similarity to be classified as near duplicates.";


    $("#classification").textContent =
        isDuplicate
            ? "NEAR DUPLICATE"
            : "NOT DUPLICATE";


    $("#classification").style.color =
        isDuplicate
            ? "var(--green)"
            : "var(--orange)";


    $("#shingleScore").textContent =
        Math.round(
            result.shingleSimilarity * 100
        ) + "%";


    $("#dpScore").textContent =
        Math.round(
            result.dpSimilarity * 100
        ) + "%";


    $("#processingTime").textContent =
        result.processingTime.toFixed(2)
        + " ms";


    /*
       SVG progress ring
    */

    const circle =
        $("#scoreProgress");

    const circumference =
        327;


    circle.style.strokeDashoffset =
        circumference -
        (
            result.score *
            circumference
        );


    circle.style.stroke =
        isDuplicate
            ? "var(--green)"
            : "var(--primary)";


    addHistory(
        question1,
        question2,
        percentage,
        isDuplicate
    );


    updateStatistics();


    resultPanel.scrollIntoView({
        behavior: "smooth",
        block: "center"
    });

}


/* =========================================
   MAIN COMPARE BUTTON
========================================= */

$("#compareBtn").addEventListener(
    "click",
    () => {

        displayResult(
            $("#question1").value,
            $("#question2").value
        );

    }
);


/* =========================================
   QUICK COMPARE
========================================= */

$("#quickCompare").addEventListener(
    "click",
    () => {

        const q1 =
            $("#quickQ1").value;

        const q2 =
            $("#quickQ2").value;


        if (
            !q1.trim() ||
            !q2.trim()
        ) {

            showToast(
                "Enter both questions first."
            );

            return;
        }


        $("#question1").value =
            q1;

        $("#question2").value =
            q2;


        updateCharacterCounts();

        showSection("compare");

        setTimeout(() => {

            displayResult(
                q1,
                q2
            );

        }, 100);

    }
);


/* =========================================
   CLEAR COMPARISON
========================================= */

$("#clearBtn").addEventListener(
    "click",
    () => {

        $("#question1").value = "";
        $("#question2").value = "";

        $("#count1").textContent = "0";
        $("#count2").textContent = "0";

        $("#resultPanel").classList.add(
            "hidden"
        );

    }
);


/* =========================================
   CHARACTER COUNTERS
========================================= */

function updateCharacterCounts() {

    $("#count1").textContent =
        $("#question1").value.length;

    $("#count2").textContent =
        $("#question2").value.length;

}


$("#question1").addEventListener(
    "input",
    updateCharacterCounts
);


$("#question2").addEventListener(
    "input",
    updateCharacterCounts
);


/* =========================================
   HISTORY
========================================= */

function addHistory(
    q1,
    q2,
    similarity,
    duplicate
) {

    const item = {

        q1: q1,
        q2: q2,

        similarity:
            similarity,

        duplicate:
            duplicate,

        time:
            new Date().toLocaleTimeString(
                [],
                {
                    hour: "2-digit",
                    minute: "2-digit"
                }
            )

    };


    comparisonHistory.unshift(item);


    // Keep last 20
    comparisonHistory =
        comparisonHistory.slice(
            0,
            20
        );


    localStorage.setItem(
        "comparisonHistory",
        JSON.stringify(
            comparisonHistory
        )
    );


    renderHistory();

}


function renderHistory() {

    const table =
        $("#recentTable");


    if (
        comparisonHistory.length === 0
    ) {

        table.innerHTML = `
            <tr class="empty-row">
                <td colspan="4">
                    No comparisons yet.
                </td>
            </tr>
        `;

        return;
    }


    table.innerHTML =
        comparisonHistory
            .map(item => {

                return `
                    <tr>

                        <td>
                            ${escapeHTML(
                                item.q1
                            )}
                            <br>
                            <span style="color:var(--muted)">
                                ${escapeHTML(
                                    item.q2
                                )}
                            </span>
                        </td>

                        <td>
                            <strong>
                                ${item.similarity}%
                            </strong>
                        </td>

                        <td>

                            <span class="result-pill ${
                                item.duplicate
                                    ? "near"
                                    : "not"
                            }">

                                ${
                                    item.duplicate
                                        ? "Near Duplicate"
                                        : "Not Duplicate"
                                }

                            </span>

                        </td>

                        <td>
                            ${item.time}
                        </td>

                    </tr>
                `;

            })
            .join("");

}


$("#clearHistory").addEventListener(
    "click",
    () => {

        comparisonHistory = [];

        localStorage.removeItem(
            "comparisonHistory"
        );

        renderHistory();

        updateStatistics();

        showToast(
            "Comparison history cleared."
        );

    }
);


/* =========================================
   STATISTICS
========================================= */

function updateStatistics() {

    const total =
        comparisonHistory.length;


    const near =
        comparisonHistory.filter(
            item => item.duplicate
        ).length;


    const not =
        total - near;


    const average =
        total === 0
            ? 0
            : comparisonHistory.reduce(
                (
                    sum,
                    item
                ) =>
                    sum +
                    item.similarity,
                0
            ) / total;


    $("#totalComparisons").textContent =
        total;


    $("#nearDuplicates").textContent =
        near;


    $("#notDuplicates").textContent =
        not;


    $("#averageSimilarity").textContent =
        Math.round(average) + "%";

}


/* =========================================
   CSV UPLOAD
========================================= */

$("#csvFile").addEventListener(
    "change",
    event => {

        const file =
            event.target.files[0];


        if (!file) {
            return;
        }


        if (
            !file.name
                .toLowerCase()
                .endsWith(".csv")
        ) {

            showToast(
                "Please select a CSV file."
            );

            return;
        }


        selectedCSV = file;


        $("#fileName").textContent =
            file.name;


        $("#analyzeCSV").disabled =
            false;


        showToast(
            "CSV file selected."
        );

    }
);


/* =========================================
   CSV PARSER
========================================= */

function parseCSV(text) {

    const rows = [];

    let row = [];

    let field = "";

    let insideQuotes = false;


    for (
        let i = 0;
        i < text.length;
        i++
    ) {

        const char =
            text[i];

        const next =
            text[i + 1];


        if (
            char === '"' &&
            insideQuotes &&
            next === '"'
        ) {

            field += '"';

            i++;

        }

        else if (char === '"') {

            insideQuotes =
                !insideQuotes;

        }

        else if (
            char === "," &&
            !insideQuotes
        ) {

            row.push(field);

            field = "";

        }

        else if (
            (char === "\n" ||
             char === "\r") &&
            !insideQuotes
        ) {

            if (
                char === "\r" &&
                next === "\n"
            ) {
                i++;
            }

            row.push(field);

            rows.push(row);

            row = [];

            field = "";

        }

        else {

            field += char;

        }

    }


    if (
        field.length ||
        row.length
    ) {

        row.push(field);

        rows.push(row);

    }


    return rows;
}


/* =========================================
   ANALYZE CSV
========================================= */

$("#analyzeCSV").addEventListener(
    "click",
    () => {

        if (!selectedCSV) {

            showToast(
                "Select a CSV file first."
            );

            return;
        }


        const reader =
            new FileReader();


        reader.onload = event => {

            const rows =
                parseCSV(
                    event.target.result
                );


            if (rows.length < 2) {

                showToast(
                    "CSV file has no data."
                );

                return;
            }


            batchResults = [];


            /*
                Expected dataset structure:

                id,
                question_1,
                question_2,
                actual_label,
                topic
            */

            for (
                let i = 1;
                i < rows.length;
                i++
            ) {

                const row =
                    rows[i];


                if (
                    row.length < 3
                ) {
                    continue;
                }


                const q1 =
                    row[1] || "";


                const q2 =
                    row[2] || "";


                if (
                    !q1.trim() ||
                    !q2.trim()
                ) {
                    continue;
                }


                const result =
                    analyzeQuestions(
                        q1,
                        q2
                    );


                const similarity =
                    Math.round(
                        result.score * 100
                    );


                const duplicate =
                    result.score >= 0.50;


                batchResults.push({

                    id:
                        row[0] ||
                        i,

                    q1:
                        q1,

                    q2:
                        q2,

                    similarity:
                        similarity,

                    duplicate:
                        duplicate,

                    actual:
                        row[3] || ""

                });

            }


            renderBatchResults();

            showToast(
                `${batchResults.length} pairs analyzed.`
            );

        };


        reader.readAsText(
            selectedCSV
        );

    }
);


/* =========================================
   BATCH RESULTS
========================================= */

function renderBatchResults() {

    const table =
        $("#batchTable");


    if (
        batchResults.length === 0
    ) {

        table.innerHTML = `
            <tr class="empty-row">
                <td colspan="5">
                    No valid question pairs found.
                </td>
            </tr>
        `;

        return;
    }


    const near =
        batchResults.filter(
            item => item.duplicate
        ).length;


    const not =
        batchResults.length - near;


    const average =
        batchResults.reduce(
            (
                sum,
                item
            ) =>
                sum +
                item.similarity,
            0
        ) /
        batchResults.length;


    $("#batchStats")
        .classList.remove("hidden");


    $("#batchTotal").textContent =
        batchResults.length;


    $("#batchNear").textContent =
        near;


    $("#batchNot").textContent =
        not;


    $("#batchAverage").textContent =
        Math.round(average) + "%";


    table.innerHTML =
        batchResults
            .map(
                (
                    item,
                    index
                ) => {

                    return `
                        <tr>

                            <td>
                                ${index + 1}
                            </td>

                            <td>
                                ${escapeHTML(
                                    truncate(
                                        item.q1,
                                        75
                                    )
                                )}
                            </td>

                            <td>
                                ${escapeHTML(
                                    truncate(
                                        item.q2,
                                        75
                                    )
                                )}
                            </td>

                            <td>
                                <strong>
                                    ${item.similarity}%
                                </strong>
                            </td>

                            <td>

                                <span class="result-pill ${
                                    item.duplicate
                                        ? "near"
                                        : "not"
                                }">

                                    ${
                                        item.duplicate
                                            ? "Near Duplicate"
                                            : "Not Duplicate"
                                    }

                                </span>

                            </td>

                        </tr>
                    `;

                }
            )
            .join("");


    $("#downloadResults").disabled =
        false;

}


/* =========================================
   DOWNLOAD RESULTS
========================================= */

$("#downloadResults").addEventListener(
    "click",
    () => {

        if (
            batchResults.length === 0
        ) {
            return;
        }


        let csv =
            "id,question_1,question_2,similarity,predicted_label\n";


        batchResults.forEach(
            item => {

                csv +=
                    `${item.id},` +
                    `"${csvEscape(item.q1)}",` +
                    `"${csvEscape(item.q2)}",` +
                    `${item.similarity},` +
                    `"${item.duplicate
                        ? "Near Duplicate"
                        : "Not Duplicate"}"\n`;

            }
        );


        const blob =
            new Blob(
                [csv],
                {
                    type:
                        "text/csv"
                }
            );


        const url =
            URL.createObjectURL(
                blob
            );


        const a =
            document.createElement(
                "a"
            );


        a.href = url;

        a.download =
            "near_duplicate_results.csv";


        document.body.appendChild(a);

        a.click();

        a.remove();

        URL.revokeObjectURL(url);


        showToast(
            "Results downloaded."
        );

    }
);


/* =========================================
   UTILITIES
========================================= */

function truncate(
    text,
    length
) {

    if (
        text.length <= length
    ) {
        return text;
    }

    return (
        text.substring(
            0,
            length
        ) + "..."
    );

}


function csvEscape(text) {

    return String(text)
        .replace(/"/g, '""');

}


function escapeHTML(text) {

    const div =
        document.createElement(
            "div"
        );

    div.textContent =
        text;

    return div.innerHTML;

}


/* =========================================
   TOAST
========================================= */

let toastTimer;


function showToast(message) {

    const toast =
        $("#toast");


    $("#toastMessage")
        .textContent = message;


    toast.classList.add("show");


    clearTimeout(
        toastTimer
    );


    toastTimer =
        setTimeout(
            () => {

                toast.classList.remove(
                    "show"
                );

            },
            2500
        );

}


/* =========================================
   INITIALIZE
========================================= */

renderHistory();

updateStatistics();

updateCharacterCounts();


/* =========================================
   DEMO DATA
========================================= */

/*
   Uncomment these lines if you want
   the dashboard to start with a demo
   comparison.

   Otherwise keep them commented.

*/

// $("#quickQ1").value =
//     "What is a binary search tree?";

// $("#quickQ2").value =
//     "Explain binary search trees.";