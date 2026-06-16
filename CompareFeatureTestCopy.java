
Skip to content
Pull requests
Issues
Marketplace
Explore
@elenakurdupova
Learn Git and GitHub without any code!

Using the Hello World guide, you’ll start a branch, write comments, and open a pull request.
BigBrassBand /
jira-git-plugin
Private

3
2

    0

Code
Issues
Pull requests 18
Actions
Projects
Wiki
Security 1
Insights

    Settings

jira-git-plugin/code/src/test/java/it/CompareFeatureTest.java /
@denis-bigbrassband-com
denis-bigbrassband-com GIT-2276 Renamed packages from 'xiplink' to 'bigbrassband'
Latest commit 0423ecb on Sep 8, 2018
History
6 contributors
@IrinaSvirkina
@ababilo
@chivorotkiv
@zugzug90
@denis-bigbrassband-com
@ddemidov-issart
We found a potential security vulnerability in one of your dependencies.

You can see this message because you have been granted access to Dependabot alerts for this repository.
692 lines (601 sloc) 36.9 KB
package it;

import com.bigbrassband.jira.git.TestMode;
import com.bigbrassband.jira.git.services.gitviewer.GitViewerTab;
import it.entity.CodeReviewSummary;
import it.entity.CommitInfo;
import it.entity.DiffLine;
import it.entity.FileDiff;
import it.entity.FullFileChangesInfo;
import it.pages.AuthPage;
import it.pages.AuthPageException;
import it.pages.Page;
import it.pages.gitviewer.BrowseRepositoryPage;
import it.pages.gitviewer.CodeReviewSummaryBlock;
import it.pages.gitviewer.CommitsPage;
import it.pages.gitviewer.CompareCommitsPage;
import it.pages.gitviewer.CompareDiffPage;
import it.pages.gitviewer.CompareIssuesPage;
import it.pages.gitviewer.ComparePage;
import it.pages.gitviewer.CompareSummaryPage;
import it.pages.gitviewer.GitViewerPage;
import it.pages.gitviewer.GitViewerPageException;
import it.pages.issues.CommitBlock;
import it.pages.issues.CommitLinksManipulations;
import it.util.GitPluginRestClient;
import it.util.SummaryDataLoader;
import junit.framework.Assert;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by nchernov on 24.02.2015.
 * Tests for GIT-683
 */
public class CompareFeatureTest extends CheckingTestDataPresent {

    private static final String RESOURCES_BRANCH_DIFF_PATH = "repositories/jiragit-test-data/diff.TST-3.master.txt";
    private static final String RESOURCES_BRANCH_TST2_DIFF_PATH = "repositories/jiragit-test-data/diff.TST-2.TST-3.txt";

    private static final String RESOURCES_TAG_DIFF_PATH = "repositories/jiragit-test-data/diff.v1.0.v1.1.txt";
    private static final String RESOURCES_COMBINED_DIFF_PATH = "repositories/jiragit-test-data/diff.v1.0.TST-3.txt";

    private static final String RESOURCES_SUMMARY_DIFF_TST3_VS_MASTER = "diffSummary.TST-3.master.txt";

    private static final String TEST_BRANCH_NAME = "TST-3";
    private static final String TEST_BASE_BRANCH_NAME = "TST-2";
    private static final String TEST_COMPARE_BRANCH_NAME = "master";
    private static final String TEST_COMPARE_BRANCH_NAME_2 = "branchNoneIssueKey";

    private static final String TEST_TAG_NAME = "v1.0";
    private static final String TEST_COMPARE_TAG_NAME = "v1.1";

    private static final String INFO_EMPTY_DIFF_MESSAGE = "No changes detected";
    private static final String PERMISSION_DENIED_MESSAGE = "You do not have enough permissions to perform this operation. Please contact your JIRA administrator.";
    private static final String UNEXISTENT_REPO_MESSAGE = "Repository with this id does not exist. Please try to reach necessary repository by menu instead of direct URL.";
    private static final String INVALID_NUMBER_ERROR = "Invalid number";
    private static final String WRONG_BRANCH_NAME_ERROR = "Branch does not exist:";

    private static final Integer UNEXISTENT_REPO_ID = 10000;
    private static final String UNEXISTENT_BRANCH_NAME = "unexistentBranch";

    private static ObjectMapper summaryMapper = new ObjectMapper();

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void prepareTestData() throws Exception {
        Assume.assumeTrue(TestMode.isRegularOrGreate());
        CheckingTestDataPresent.prepareTestData();

        summaryMapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, true);
        summaryMapper.configure(SerializationConfig.Feature.AUTO_DETECT_GETTERS, true);
        summaryMapper.configure(SerializationConfig.Feature.AUTO_DETECT_FIELDS, true);
    }

    @Before
    public void setUp() {
        System.out.println(testName.getMethodName() + " running ...");
    }

    // ------------------- Test Diff tab --------------
    @Test
    public void testDropdownWorksForCommitId() throws IOException, GitViewerPageException {
        List<CommitInfo> expectedOfBranch = repositoryManager.getCommitsForBranch(TEST_BASE_BRANCH_NAME);
        String branchCommitId = expectedOfBranch.get(0).getId();
        CompareDiffPage compareDiffPage = new CompareDiffPage(driver, baseUrl, testRepositoryId);
        compareDiffPage.openForCommitId(branchCommitId, "", "");
        compareDiffPage.switchBaseBranch(TEST_BRANCH_NAME);

        File diffFile = new File(CompareFeatureTest.class.getClassLoader().getResource(RESOURCES_BRANCH_TST2_DIFF_PATH).getFile());
        List<FileDiff> fileDiffs = parseForDiff(diffFile);
        compareDiffs(fileDiffs, compareDiffPage.getDiff());

        String selectedBaseValue = compareDiffPage.getSelectedCompareValue();
        Assert.assertEquals("Incorrect base commit is selected", branchCommitId, selectedBaseValue);

        List<String> topLevelValues = compareDiffPage.getDropdownCompareTopLevelValues();
        Assert.assertEquals("Must be two top-level sections: branches and tags", 3, topLevelValues.size());
        Assert.assertEquals("The first top-level section must be commitId", branchCommitId, topLevelValues.get(0));
        Assert.assertEquals("The second top-level section must be branches", "Branches", topLevelValues.get(1));
        Assert.assertEquals("The third top-level section must b etags", "Tags", topLevelValues.get(2));
    }

    @Test
    public void testDiffTabBranches() throws IOException {
        File diffFile = new File(CompareFeatureTest.class.getClassLoader().getResource(RESOURCES_BRANCH_DIFF_PATH).getFile());
        List<FileDiff> fileDiffs = parseForDiff(diffFile);
        CompareDiffPage compareDiffPage = new CompareDiffPage(driver, baseUrl, testRepositoryId);
        compareDiffPage.openBranches(TEST_BRANCH_NAME, TEST_COMPARE_BRANCH_NAME);
        compareDiffs(fileDiffs, compareDiffPage.getDiff());
    }

    @Test
    public void testDiffTabTags() throws IOException {
        File diffFile = new File(CompareFeatureTest.class.getClassLoader().getResource(RESOURCES_TAG_DIFF_PATH).getFile());
        List<FileDiff> fileDiffs = parseForDiff(diffFile);
        CompareDiffPage compareDiffPage = new CompareDiffPage(driver, baseUrl, testRepositoryId);
        compareDiffPage.openTags(TEST_TAG_NAME, TEST_COMPARE_TAG_NAME);
        compareDiffs(fileDiffs, compareDiffPage.getDiff());
    }

    @Test
    public void testDiffTabNoMergeBaseCommitHash() {
        CompareDiffPage compareDiffPage = new CompareDiffPage(driver, baseUrl, testRepositoryId);
        compareDiffPage.openBranches(TEST_BRANCH_NAME, TEST_COMPARE_BRANCH_NAME);
        List<String> topLevelValues = compareDiffPage.getDropdownBaseTopLevelValues();
        Assert.assertEquals("Must be 3 top-level sections: branches and tags", 3, topLevelValues.size());
        Assert.assertEquals("The First top-level section must be empty", "", topLevelValues.get(0));
        Assert.assertEquals("The Second top-level section must be branches", "Branches", topLevelValues.get(1));
        Assert.assertEquals("The Third top-level section must be tags", "Tags", topLevelValues.get(2));
    }

    @Test
    public void testDiffTabCombinedDiff() throws IOException {
        File diffFile = new File(CompareFeatureTest.class.getClassLoader().getResource(RESOURCES_COMBINED_DIFF_PATH).getFile());
        List<FileDiff> fileDiffs = parseForDiff(diffFile);
        CompareDiffPage compareDiffPage = new CompareDiffPage(driver, baseUrl, testRepositoryId);
        compareDiffPage.openForDiff("", TEST_BRANCH_NAME, TEST_TAG_NAME, "");
        compareDiffs(fileDiffs, compareDiffPage.getDiff());
    }

    @Test
    @Ignore //Covered by BaseParamsValidatorTest
    public void testPermissionsForPage() throws IOException {
        Assume.assumeTrue(TestMode.isAll()); //Covered by CompareActionRequestHandlerTest

        AuthPage authPage = new AuthPage(driver, baseUrl);
        authPage.auth(TEST_USER_NONADMIN_LOGIN, TEST_USER_NONADMIN_PASSWORD);

        try {
            CompareDiffPage compareDiffPage = new CompareDiffPage(driver, baseUrl, testRepositoryId);
            compareDiffPage.openBranches(TEST_BRANCH_NAME, TEST_COMPARE_BRANCH_NAME);
            Assert.assertTrue("Diff content not allowed for non admin, nor developer user", compareDiffPage.getDiff().isEmpty());
            Assert.assertEquals("Wrong error message", PERMISSION_DENIED_MESSAGE, compareDiffPage.getErrorMessage());

            CompareSummaryPage summaryPage = new CompareSummaryPage(driver, baseUrl, testRepositoryId);
            summaryPage.openBranches(TEST_BRANCH_NAME, TEST_COMPARE_BRANCH_NAME);
            Assert.assertTrue("Diff content not allowed for non admin, nor developer user", summaryPage.navigateToSummary().isEmpty());
            Assert.assertEquals("Wrong error message", PERMISSION_DENIED_MESSAGE, summaryPage.getErrorMessage());

            CompareCommitsPage commitsPage = new CompareCommitsPage(driver, baseUrl, testRepositoryId);
            summaryPage.openBranches(TEST_BRANCH_NAME, TEST_COMPARE_BRANCH_NAME);
            Assert.assertTrue("Diff content not allowed for non admin, nor developer user", summaryPage.navigateToSummary().isEmpty());
            Assert.assertEquals("Wrong error message", PERMISSION_DENIED_MESSAGE, summaryPage.getErrorMessage());

        } finally {
            //re-login as admin for other tests working correctly
            authPage.auth(ADMIN_USERNAME, ADMIN_PASSWORD);
        }
    }

    @Test
    public void testDiffTabNoCompareBranchSelected() throws IOException {
        Assume.assumeTrue(TestMode.isRegularOrGreate());

        CompareDiffPage compareDiffPage = new CompareDiffPage(driver, baseUrl, testRepositoryId);
        compareDiffPage.openBranches("", "");
        Assert.assertTrue("Diff content should be empty for this case", compareDiffPage.getDiff().isEmpty());
        // If no base/compare branch is selected -- master branch is used and there are no errors
        Assert.assertFalse("No warning icon displayed for empty compare branch", compareDiffPage.isWarningIconVisible());
        Assert.assertFalse("Dropdown border should be highlighted", compareDiffPage.getCompareBranchesDropdownBorderWidth() > 0);
    }

    @Test
    public void testDiffTabEmptyDiff() throws IOException {
        Assume.assumeTrue(TestMode.isRegularOrGreate());

        CompareDiffPage compareDiffPage = new CompareDiffPage(driver, baseUrl, testRepositoryId);
        compareDiffPage.openBranches(TEST_COMPARE_BRANCH_NAME, TEST_BRANCH_NAME);
        Assert.assertTrue("Diff content should be empty for this case", compareDiffPage.getDiff().isEmpty());
        Assert.assertEquals("Wrong info message", INFO_EMPTY_DIFF_MESSAGE, compareDiffPage.getInfoMessage());
    }

    @Test
    public void testCompare_NotEmptyDiff_NoneIssue() throws IOException {
        Assume.assumeTrue(TestMode.isRegularOrGreate());

        CompareIssuesPage compareDiffPage = new CompareIssuesPage(driver, baseUrl, testRepositoryId);
        compareDiffPage.openBranches(TEST_COMPARE_BRANCH_NAME, TEST_COMPARE_BRANCH_NAME_2);
        WebElement content = compareDiffPage.getContentWrapper();
        Assert.assertEquals("Referenced Jira issues\n" +
                "No referring issues found", content.getText());
    }

    @Test
    @Ignore //Covered by BaseParamsValidatorTest
    public void testDiffRedirection() {
        Assume.assumeTrue(TestMode.isAll()); //Covered by CompareActionRequestHandlerTest
        assertRedirection(new CompareDiffPage(driver, baseUrl, testRepositoryId), GitViewerTab.COMPARE_DIFF.getUrl());
    }

    @Test
    public void testDiffErrorMessage() {
        Assume.assumeTrue(TestMode.isRegularOrGreate());
        assertRepoidIsNotANumber(new CompareDiffPage(driver, baseUrl, testRepositoryId), GitViewerTab.COMPARE_DIFF.getUrl());
    }

    @Test
    public void testDiffTabSettingsSaved() throws IOException, AuthPageException, URISyntaxException {
        assertLastTabSaved(new CompareCommitsPage(driver, baseUrl, testRepositoryId), GitViewerTab.COMPARE_COMMITS);
        assertLastTabSaved(new CompareSummaryPage(driver, baseUrl, testRepositoryId), GitViewerTab.COMPARE_SUMMARY);
        assertLastTabSaved(new CompareDiffPage(driver, baseUrl, testRepositoryId), GitViewerTab.COMPARE_DIFF);
        assertLastTabSaved(new BrowseRepositoryPage(driver, baseUrl, testRepositoryId), GitViewerTab.BROWSE_GIT);
        assertLastTabSaved(new CommitsPage(driver, baseUrl, testRepositoryId), GitViewerTab.COMMITS);
    }

    public void assertLastTabSaved(GitViewerPage gitViewerPage, GitViewerTab gitViewerTab) throws AuthPageException, URISyntaxException {
        gitViewerPage.open();
        assertLastTabSavedForMenuDropdown(gitViewerPage, gitViewerTab);
    }

    public void assertLastTabSavedForMenuDropdown(GitViewerPage gitViewerPage, GitViewerTab gitViewerTab) {
        Page loginPage = new AuthPage(driver, baseUrl);
        GitViewerPage restoredGitViewerPage = loginPage.navigateToGitViewer(testRepositoryId, testRepoName);
        String url = restoredGitViewerPage.getCurrentUrl();
        url = url.substring(0, url.indexOf("?"));
        Assert.assertTrue("Wrong last selected tab saved, expected: " + gitViewerTab.getUrl(), url.endsWith(gitViewerTab.getUrl()));
    }

    public void compareDiffs(List<FileDiff> expected, List<FileDiff> actual) {
        Assert.assertTrue(actual.size() > 0);
        Assert.assertEquals(actual.size(), expected.size());
        int counter = 0;
        for (FileDiff fileDiff : actual) {
            Assert.assertEquals(expected.get(counter), fileDiff);
            counter++;
        }
    }

    /**
     * Util method parsing test data file with tab delimeted diff obtained from CompareDiff page
     *
     * @param txtFile with format:
     *                <pre>
     *                [filename]\n<br>
     *                [lineNumber]\t[lineNumber]\t[content]\n<br>
     *                [lineNumber]\t[lineNumber]\t[content]\n
     *                ...
     *                </pre>
     * @return List of FileDiff objects
     */
    private static List<FileDiff> parseForDiff(File txtFile) throws IOException {
        FileReader fileReader = new FileReader(txtFile);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String line = null;
        List<FileDiff> fileDiffs = new ArrayList<>();
        FileDiff currentFileDiff = null;
        while ((line = bufferedReader.readLine()) != null) {
            String[] tokens = line.split("\t", -1);
            if (tokens.length <= 3 && tokens.length > 1) {
                String firstCol = tokens[0].replaceAll(" ", "");
                String secondCol = tokens[1].replaceAll(" ", "");
                String content = " ";
                if (tokens.length > 2) {
                    content = tokens[2];
                }
                int lineNumber = StringUtils.isEmpty(firstCol) ? Integer.parseInt(secondCol) : Integer.parseInt(firstCol);
                boolean added, deleted;
                added = deleted = false;
                if (StringUtils.isEmpty(firstCol) && !StringUtils.isEmpty(secondCol)) {
                    added = true;
                }
                if (!StringUtils.isEmpty(firstCol) && StringUtils.isEmpty(secondCol)) {
                    deleted = true;
                }
                DiffLine diffLine = new DiffLine(lineNumber, content, added, deleted);
                currentFileDiff.getDiff().add(diffLine);
            } else {
                if (currentFileDiff != null) {
                    fileDiffs.add(currentFileDiff);
                }
                FileDiff fileDiff = new FileDiff();
                fileDiff.setFileName(line);
                fileDiff.setDiff(new ArrayList<DiffLine>());
                currentFileDiff = fileDiff;
            }
        }
        if (currentFileDiff != null) {
            fileDiffs.add(currentFileDiff);
        }
        bufferedReader.close();
        fileReader.close();
        return fileDiffs;
    }

    // ------------------- Test Summary tab --------------
    @Test
    public void testSummaryTabSuccess() throws IOException {
        CompareSummaryPage currentPage = new CompareSummaryPage(driver, baseUrl, testRepositoryId);
        currentPage.openBranches(TEST_BRANCH_NAME, TEST_COMPARE_BRANCH_NAME);
        CodeReviewSummary summaryInfo = currentPage.navigateToSummary().parse();

        //CodeReviewSummary expetcedSummary = diffSummaryTST3MasterGeneration();
        CodeReviewSummary expetcedSummary = readSummaryFromStream(RESOURCES_SUMMARY_DIFF_TST3_VS_MASTER, "Read summary for diff TST-3..master");
        // Commits block
        Assert.assertEquals(expetcedSummary.getFirstCommit(), summaryInfo.getFirstCommit());
        Assert.assertEquals(expetcedSummary.getLastCommit(), summaryInfo.getLastCommit());
        Assert.assertEquals(expetcedSummary.getCommitsNote(), summaryInfo.getCommitsNote());
        Assert.assertEquals(expetcedSummary.getFilesSummary(), summaryInfo.getFilesSummary());
        Assert.assertEquals(expetcedSummary.getLinesSummary()[0], summaryInfo.getLinesSummary()[0]);
        Assert.assertEquals(expetcedSummary.getLinesSummary()[1], summaryInfo.getLinesSummary()[1]);
        Assert.assertEquals(expetcedSummary.getLinesSummary()[2], summaryInfo.getLinesSummary()[2]);

        // Aggregated Lines by Developers block
        Assert.assertEquals(expetcedSummary.getLinesByDeveloper(), summaryInfo.getLinesByDeveloper());

        // Files block
        Assert.assertEquals(expetcedSummary.getFilesSummary(), summaryInfo.getFilesSummary());
    }

    @Test
    public void testSummaryTabOverflow() throws Exception {
        CompareSummaryPage currentPage = new CompareSummaryPage(driver, baseUrl, testRepositoryId);
        currentPage.openBranches(TEST_BRANCH_NAME, TEST_COMPARE_BRANCH_NAME);
        WebElement contentWrapper = currentPage.getContentWrapper();
        Assert.assertTrue("Wrapper must be narrow enough", contentWrapper.getSize().getWidth() < 3000);
        driver.executeScript(
                String.format("AJS.$('[class=\"%s\"]').prepend(AJS.$('<div style=\"width:4000px\"></div>'))",
                        contentWrapper.getAttribute("class")));
        Assert.assertTrue("Wrapper must still be narrow enough", contentWrapper.getSize().getWidth() < 3000);
    }

    // the function is used for "diffSummary.TST-3.master.txt" generation
    private CodeReviewSummary diffSummaryTST3MasterGeneration() throws IOException {
        CodeReviewSummary expetcedSummary = new CodeReviewSummary();
        expetcedSummary.setFirstCommit("November 28, 2014"); //but githug shows November 27
        expetcedSummary.setLastCommit("November 28, 2014");
        expetcedSummary.setCommitsNote("6 - jiragit-test-data");
        expetcedSummary.setFilesSummary("0 added, 4 changed, 1 deleted");
        //note: github says just about added and deleted, so
        //    github.added=gitplugin.added+gitplugin.updated
        //    github.deleted=gitplugin.deleted+gitplugin.updated
        // see https://github.com/AndreyLevchenko/jiragit-test-data/compare/TST-3...master?diff=split&name=master
        //github total added=12+7+9+0    !=   gitplugin total added+updated=17+19
        expetcedSummary.setLinesSummary(16, 12, 17);
        expetcedSummary.addDeveloperInfo("Andrey Levchenko", 15, 12, 16);
        expetcedSummary.setTotalfilesInfo(Arrays.asList(
                new FullFileChangesInfo("src/main/java/com/example/my/test/application/MainModule.java", 6, 6, 6, false, false, 109, 109),
                new FullFileChangesInfo("src/main/resources/images/git jobs plan.JPG", null, null, null, false, true, null, null),
                new FullFileChangesInfo("src/main/resources/js/galleria.flickr.js", 2, 5, 4, false, false, 223, 221),
                new FullFileChangesInfo("src/main/resources/js/searchChangeValue.js", 8, 1, null, false, false, 70, 78),
                new FullFileChangesInfo("src/main/resources/js/utility.js", null, null, 7, false, false, 28, 21)
        ));

        //write result to file
        writeSummaryToStream("src/test/resources/repositories/jiragit-test-data/diffSummary.TST-3.master.txt", expetcedSummary);

        return expetcedSummary;
    }

    private CodeReviewSummary readSummaryFromStream(String expectedSummaryFile, String msgOfOperation) {
        return SummaryDataLoader.readSummaryFromStream(testRepoName, expectedSummaryFile, msgOfOperation);
    }

    private void writeSummaryToStream(String fileName, CodeReviewSummary serialize) throws IOException {
        summaryMapper.defaultPrettyPrintingWriter().writeValue(new File(fileName), serialize);
    }

    @Test
    public void testSummaryTabEmptyDiff() {
        CompareSummaryPage currentPage = new CompareSummaryPage(driver, baseUrl, testRepositoryId);
        currentPage.openBranches(TEST_COMPARE_BRANCH_NAME, TEST_COMPARE_BRANCH_NAME);
        Assert.assertTrue("Summary content is not empty for empty diff", currentPage.navigateToSummary().parse().isEmpty());
        Assert.assertEquals("Wrong info message", INFO_EMPTY_DIFF_MESSAGE, currentPage.getInfoMessage());
    }

    @Test
    public void testSummaryTabNoCompareBranchSelected() {
        //shows red triange if compare branch isn't selected
        //http://ws105:2990/jira/secure/bbb.gp.gitviewer.CompareDiffGit.jspa?repoId=1&branchName=master&tagName=&compareBranch=&compareTag=
        CompareSummaryPage currentPage = new CompareSummaryPage(driver, baseUrl, testRepositoryId);
        currentPage.openBranches("", "");
        CodeReviewSummaryBlock summaryBlock = currentPage.navigateToSummary();
        Assert.assertTrue("Diff content should be empty for this case", summaryBlock.parse().isEmpty());
        // If no base/compare branch is selected -- master branch is used and there are no errors
        Assert.assertFalse("No warning icon displayed for empty compare branch", currentPage.isWarningIconVisible());
        Assert.assertFalse("Dropdown border should be highlighted", currentPage.getCompareBranchesDropdownBorderWidth() > 0);
    }

    @Test
    public void testSummaryRedirection() {
        //empty query params - http://ws105:2990/jira/secure/bbb.gp.gitviewer.CompareDiffGit.jspa
        //here is redirection, so no error message
        assertRedirection(new CompareSummaryPage(driver, baseUrl, testRepositoryId), GitViewerTab.COMPARE_SUMMARY.getUrl());
    }

    private void assertRedirection(ComparePage page, String basePageUrl) {
        page.open(basePageUrl);
        Assert.assertNull(page.getErrorMessage());
    }

    @Test
    public void testSummaryErrorMessage() {
        //any error, ex.: repoId is not a number
        assertRepoidIsNotANumber(new CompareSummaryPage(driver, baseUrl, testRepositoryId), GitViewerTab.COMPARE_SUMMARY.getUrl());
    }

    private void assertRepoidIsNotANumber(ComparePage page, String basePageUrl) {
        page.open(basePageUrl + "?repoId=notANumber");
        Assert.assertTrue("Diff content should be empty for this case", page.isEmpty());
        Assert.assertTrue("Wrong info message", page.getErrorMessage().startsWith(INVALID_NUMBER_ERROR));
    }

    @Test
    public void testSummaryTabUrl() {
        //Covered by CompareActionRequestHandlerTest:
        //unexistent branch - ?repoId=100&branchName=notExistent&tagName=&compareBranch=release&compareTag=
        //unexistent compareBranch - ?repoId=100&branchName=master&tagName=&compareBranch=notExistent&compareTag=

        //repoidIsNotANumber - see testSummaryErrorMessage and assertRepoidIsNorANumber

        //unexistent repoid - ?repoId=100&branchName=master&tagName=&compareBranch=release&compareTag=
        CompareSummaryPage page = new CompareSummaryPage(driver, baseUrl, UNEXISTENT_REPO_ID);
        page.openBranches(TEST_BRANCH_NAME, TEST_COMPARE_BRANCH_NAME);
        Assert.assertTrue("Diff content should be empty for this case", page.isEmpty());
        Assert.assertEquals("Wrong info message", UNEXISTENT_REPO_MESSAGE, page.getErrorMessage());

        //todo: check strange symbols in branchName (http://git-scm.com/docs/git-check-ref-format). Are they escaped?
    }

    // ------------------- Test Commits tab --------------
    @Test
    public void testCommitsTabMenu() {
        // check, that number in menu = 0 in case of empty diff
        CompareSummaryPage currentPage = new CompareSummaryPage(driver, baseUrl, testRepositoryId);
        currentPage.openBranches(TEST_COMPARE_BRANCH_NAME, TEST_COMPARE_BRANCH_NAME);
        Assert.assertEquals(0, currentPage.navigateToMenu().getCommitsNumber());

        // check, that number in menu = number of commits
        currentPage.openBranches(TEST_BRANCH_NAME, TEST_COMPARE_BRANCH_NAME);
        Assert.assertEquals(6, currentPage.navigateToMenu().getCommitsNumber());

        // check, that number in menu = 0 when no branch is selected
        currentPage = new CompareSummaryPage(driver, baseUrl, testRepositoryId);
        currentPage.openBranches(TEST_COMPARE_BRANCH_NAME, "");
        Assert.assertEquals(0, currentPage.navigateToMenu().getCommitsNumber());

    }

    @Test
    public void testCommitsTabUrl() {
        Assume.assumeTrue(TestMode.isRegularOrGreate());

        CompareCommitsPage page = new CompareCommitsPage(driver, baseUrl, UNEXISTENT_REPO_ID);
        page.openBranches(TEST_BRANCH_NAME, TEST_COMPARE_BRANCH_NAME);
        Assert.assertTrue("Diff content should be empty for this case", page.isEmpty());
        Assert.assertEquals("Wrong info message", UNEXISTENT_REPO_MESSAGE, page.getErrorMessage());
    }

    @Test
    public void testCommitsEmptyDiff() {
        Assume.assumeTrue(TestMode.isRegularOrGreate());

        // shows nothing
        CompareCommitsPage currentPage = new CompareCommitsPage(driver, baseUrl, testRepositoryId);
        currentPage.openBranches(TEST_COMPARE_BRANCH_NAME, TEST_COMPARE_BRANCH_NAME);
        Assert.assertTrue("Summary content is not empty for empty diff", currentPage.isEmpty());
        Assert.assertEquals("Wrong info message", INFO_EMPTY_DIFF_MESSAGE, currentPage.getInfoMessage());
    }

    @Test
    @Ignore //Covered by BaseParamsValidatorTest
    public void testCommitRedirection() {
        Assume.assumeTrue(TestMode.isAll()); //Covered by CompareActionRequestHandlerTest
        assertRedirection(new CompareCommitsPage(driver, baseUrl, testRepositoryId), GitViewerTab.COMPARE_COMMITS.getUrl());
    }

    @Test
    public void testCommitsErrorMessage() {
        Assume.assumeTrue(TestMode.isRegularOrGreate());
        assertRepoidIsNotANumber(new CompareCommitsPage(driver, baseUrl, testRepositoryId), GitViewerTab.COMPARE_COMMITS.getUrl());
    }

    @Test
    public void testCompareCommitsSuccess() throws ParseException {
        //Compare commits does't have list of branches (AJS.$(".gp-branch"))
        CompareCommitsPage currentPage = new CompareCommitsPage(driver, baseUrl, testRepositoryId);
        currentPage.openBranches("TST-2", "TST-3");

        List<WebElement> branches = driver.findElements(By.className(".gp-branch"));
        Assert.assertTrue(branches.isEmpty());

        //Compare commits shows just "ahead commits".
        CommitBlock commit_TST3_1 = currentPage.getCommitBlock("c73abc3ae3d81fe93d23fca9e404aa30741dd566");
        CommitBlock commit_TST3_2 = currentPage.getCommitBlock("4d2ba93a508605b400a8fe4dea59a595325a65c5");
        CommitBlock commit_TST3_3 = currentPage.getCommitBlock("2d41f0fc6c1e0f0e9542397bc55e7d04b4dc001d");
        Assert.assertNotNull(commit_TST3_1);
        Assert.assertNotNull(commit_TST3_2);
        Assert.assertNotNull(commit_TST3_3);

        // commit exists just in TST-2, so it isn't shown
        boolean commit_TST2_1_exists = true;
        try {
            CommitBlock commit_TST2_1 = currentPage.getCommitBlock("1622444a07b227757a3f549542fd882e8c3d1429");
        } catch (NoSuchElementException ex) {
            commit_TST2_1_exists = false;
        }
        Assert.assertFalse(commit_TST2_1_exists);

        // initial commit exists in both TST-3 and TST-2, so it isn't shown
        boolean initialCommit_exists = true;
        try {
            CommitBlock commit_TST2_1 = currentPage.getCommitBlock("1414071340ba5a2166d02e290e144ccd779c148b");
        } catch (NoSuchElementException ex) {
            initialCommit_exists = false;
        }
        Assert.assertFalse(initialCommit_exists);

        // Check content of commits.
        CommitInfo commit1 = commit_TST3_1.parse(true);
        Assert.assertEquals("Andrey Levchenko", commit1.getAuthor());
        Assert.assertEquals(Arrays.asList("src/main/java/com/example/my/test/application/MainModule.java"), commit1.getFileNames());

        CommitInfo commit2 = commit_TST3_2.parse(true);
        Assert.assertEquals("Andrey Levchenko", commit2.getAuthor());
        Assert.assertEquals(Arrays.asList("src/main/resources/js/galleria.flickr.js"), commit2.getFileNames());

        CommitInfo collapsedCommit3 = commit_TST3_3.parse(false);
        Assert.assertEquals("Andrey Levchenko", collapsedCommit3.getAuthor());
        System.out.println(collapsedCommit3.getFileNames());
        Assert.assertEquals(Arrays.asList(
                        "src/main/java/com/example/my/test/application/MainModule.java",
                        "src/main/java/com/example/my/test/application/hibernate/HibernateUtil.java",
                        "src/main/resources/js/chooser.js"),
                collapsedCommit3.getFileNames());
        Assert.assertEquals(2, collapsedCommit3.getTotalLinesAdded());
        Assert.assertEquals(39, collapsedCommit3.getTotalLinesChanged());
        Assert.assertEquals(1, collapsedCommit3.getTotalLinesDeleted());
        Assert.assertEquals(5, (int) collapsedCommit3.getDisplayedTotals().getAdded());
        Assert.assertEquals(47, (int) collapsedCommit3.getDisplayedTotals().getChanged());
        Assert.assertEquals(1, (int) collapsedCommit3.getDisplayedTotals().getDeleted());

        CommitInfo commit3 = commit_TST3_3.parse(true);
        Assert.assertEquals("Andrey Levchenko", commit3.getAuthor());
        System.out.println(commit3.getFileNames());
        Assert.assertEquals(Arrays.asList(
                        "src/main/java/com/example/my/test/application/MainModule.java",
                        "src/main/java/com/example/my/test/application/hibernate/HibernateUtil.java",
                        "src/main/resources/js/chooser.js",
                        "src/main/resources/js/form.js",
                        "src/main/resources/js/galleria.flickr.js",
                        "src/main/resources/js/searchChangeValue.js",
                        "src/main/resources/js/utility.js"),
                commit3.getFileNames());
        Assert.assertEquals(5, commit3.getTotalLinesAdded());
        Assert.assertEquals(47, commit3.getTotalLinesChanged());
        Assert.assertEquals(1, commit3.getTotalLinesDeleted());
    }

    @Test
    public void testCorrectPullRequestResolving() throws Exception {
        //1.Create pullRequest from=TST-3, to=master
        boolean issueResolved = false;
        Integer pullRequestId = null;
        try {
            String issueStatus = jiraRestClient.getIssueStatus(TEST_BRANCH_NAME);
            if (!issueStatus.equalsIgnoreCase("resolved")) {
                jiraRestClient.resolveIssue(TEST_BRANCH_NAME);
            }
            issueResolved = true;
            GitPluginRestClient.SimplifiedPullRequestData pullRequestData =  client.getPullRequestData(TEST_BRANCH_NAME, testRepositoryId, TEST_BRANCH_NAME, "master");
            pullRequestId = pullRequestData.getReasonPullRequestId();
            boolean possibleToOpen = pullRequestData.isPossibleToOpen();
            if (pullRequestId == -1) {
                GitPluginRestClient.SimplifiedPullRequestData data = client.createPullRequest(TEST_BRANCH_NAME, testRepositoryId, "master", TEST_BRANCH_NAME);
                pullRequestId = data.getId();
            }
            //2. Goto repoId=xxx&branchName=master&compareBranch=TST-3
            // check that redirect occurred, pullRequest was resolved from parameters
            CompareSummaryPage compareSummaryPage = new CompareSummaryPage(driver, baseUrl, testRepositoryId, "master", TEST_BRANCH_NAME);
            compareSummaryPage.open();
            String currentUrl = compareSummaryPage.getCurrentUrl();
            String branchName = compareSummaryPage.getSelectedBaseValue();
            String compareBranch = compareSummaryPage.getSelectedCompareValue();
            Assert.assertEquals("master", branchName);
            Assert.assertEquals(TEST_BRANCH_NAME, compareBranch);
            Assert.assertTrue("Redirect should occur with appended parameter: " + String.format("pullRequestId=%d", pullRequestId),
                    currentUrl.contains(String.format("pullRequestId=%d", pullRequestId)));

            //3. Goto repoId=xxx&branchName=TST-3&compareBranch=master
            //check no pull request was resolved, check diff is present and not empty
            //check base and compareBranch dropdowns are consistent with URL
            //check some file diffs are present at the Rollup tab
            compareSummaryPage = new CompareSummaryPage(driver, baseUrl, testRepositoryId, TEST_BRANCH_NAME, "master");
            compareSummaryPage.open();

            currentUrl = compareSummaryPage.getCurrentUrl();
            branchName = compareSummaryPage.getSelectedBaseValue();
            compareBranch = compareSummaryPage.getSelectedCompareValue();
            Assert.assertEquals(TEST_BRANCH_NAME, branchName);
            Assert.assertEquals("master", compareBranch);
            Assert.assertTrue("Redirect should not occur with appended parameter: " + String.format("pullRequestId=%d", pullRequestId),
                    !currentUrl.contains(String.format("pullRequestId=%d", pullRequestId)));

            CommitLinksManipulations commitLinksManipulations = new CommitLinksManipulations(driver);
            Assert.assertTrue("File diffs set should not be empty on current compare page", commitLinksManipulations.getFileNamesFromSummaryTab().size() > 0);
        } finally {
            //4. Close pullRequest. Re-open issue
            client.closePullRequest(pullRequestId);
            if (issueResolved) {
                jiraRestClient.reOpenIssue(TEST_BRANCH_NAME);
            }
        }
    }

    @Test
    public void testPullRequestDiff() throws Exception {
        boolean issueResolved = false;
        Integer pullRequestId = null;
        try {
            // 1. Create pullRequest from=TST-2, to=TST-3
            String issueStatus = jiraRestClient.getIssueStatus(TEST_BRANCH_NAME);
            if (!issueStatus.equalsIgnoreCase("resolved")) {
                jiraRestClient.resolveIssue(TEST_BRANCH_NAME);
            }
            issueResolved = true;
            GitPluginRestClient.SimplifiedPullRequestData pullRequestData =
                    client.getPullRequestData(TEST_BRANCH_NAME, testRepositoryId, TEST_BRANCH_NAME, TEST_BASE_BRANCH_NAME);
            pullRequestId = pullRequestData.getReasonPullRequestId();
            if (pullRequestId == -1) {
                GitPluginRestClient.SimplifiedPullRequestData data = client.createPullRequest(TEST_BRANCH_NAME, testRepositoryId, TEST_BRANCH_NAME, TEST_BASE_BRANCH_NAME);
                pullRequestId = data.getId();
            }
            Assert.assertTrue(pullRequestId > 0);

            // 2. Goto repoId=xxx&pullRequestId=zzz, check diff afterwards
            CompareDiffPage compareDiffPage = new CompareDiffPage(driver, baseUrl, testRepositoryId, pullRequestId);
            compareDiffPage.open();

            File diffFile = new File(CompareFeatureTest.class.getClassLoader().getResource(RESOURCES_BRANCH_TST2_DIFF_PATH).getFile());
            List<FileDiff> fileDiffs = parseForDiff(diffFile);
            compareDiffs(fileDiffs, compareDiffPage.getDiff());

            String selectedCompareValue = compareDiffPage.getSelectedCompareValue();
            Assert.assertEquals("Incorrect base commit is selected", TEST_BASE_BRANCH_NAME, selectedCompareValue);
            String selectedBaseValue = compareDiffPage.getSelectedBaseValue();
            Assert.assertEquals("Incorrect base commit is selected", TEST_BRANCH_NAME, selectedBaseValue);

        } finally {
            // Close pullRequest. Re-open issue
            if (pullRequestId != null) {
                client.closePullRequest(pullRequestId);
            }
            if (issueResolved) {
                jiraRestClient.reOpenIssue(TEST_BRANCH_NAME);
            }
        }
    }

}
