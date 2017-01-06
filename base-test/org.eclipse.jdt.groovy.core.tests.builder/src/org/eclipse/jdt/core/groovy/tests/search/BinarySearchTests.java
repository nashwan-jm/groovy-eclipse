/*
 * Copyright 2009-2017 the original author or authors.
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
package org.eclipse.jdt.core.groovy.tests.search;

import junit.framework.Test;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;

/**
 * @author Andrew Eisenberg
 */
public class BinarySearchTests extends AbstractGroovySearchTest {

    public static Test suite() {
        return buildTestSuite(BinarySearchTests.class);
    }

    public BinarySearchTests(String name) {
        super(name);
    }

    private String groovyClassContents =
            "package pack\n" +
            "\n" +
            "class OtherClass { }\n" +
            "class AGroovyClass {\n" +
            "        String name_1\n" +
            "        int age_1\n" +
            "        def referencedInInitializer() { }\n" +
            "        def fieldInInitializer\n" +
            "\n" +
            "        def doit() {\n" +
            "                println name_1 + age_1\n" +
            "                AGroovyClass\n" +
            "                OtherClass\n" +
            "                doit()\n" +
            "                def aClosure = {\n" +
            "                        println name_1 + age_1\n" +
            "                        AGroovyClass\n" +
            "                        OtherClass\n" +
            "                        doit()\n" +
            "                }\n" +
            "        }\n" +
            "        { \n" +
            "                referencedInInitializer() \n" +
            "                fieldInInitializer\n" +
            "        }\n" +
            "}\n" +
            "";

    private String groovyClassContents2 =
            "package pack\n" +
            "\n" +
            "class AnotherGroovyClass {\n" +
            "        def doit() {\n" +
            "                println new AGroovyClass().name_1 + new AGroovyClass().age_1\n" +
            "                AGroovyClass\n" +
            "                OtherClass\n" +
            "                new AGroovyClass().doit()\n" +
            "                def aClosure = {\n" +
            "                        println new AGroovyClass().name_1 + new AGroovyClass().age_1\n" +
            "                        AGroovyClass\n" +
            "                        OtherClass\n" +
            "                        new AGroovyClass().doit()\n" +
            "                }\n" +
            "        }\n" +
            "        { \n" +
            "                new AGroovyClass().referencedInInitializer() \n" +
            "                new AGroovyClass().fieldInInitializer\n" +
            "        }\n" +
            "}\n";

    private IJavaProject javaProject;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Path libDir = new Path(FileLocator.resolve(Platform.getBundle("org.eclipse.jdt.groovy.core.tests.builder").getEntry("lib")).getFile());
        env.addEntry(project.getFullPath(), JavaCore.newLibraryEntry(libDir.append("binGroovySearch.jar"), libDir.append("binGroovySearchSrc.zip"), null));

        javaProject = env.getJavaProject(project.getName());
        waitForIndexer(javaProject);

        // overwrite the contents vars with the actual contents
        groovyClassContents = javaProject.findType("pack.AGroovyClass").getTypeRoot().getBuffer().getContents();
        groovyClassContents2 = javaProject.findType("pack.AnotherGroovyClass").getTypeRoot().getBuffer().getContents();
    }

    @Override
    protected void tearDown() throws Exception {
        javaProject = null;
        super.tearDown();
    }

    private MockSearchRequestor performSearch(IJavaElement toSearchFor) throws Exception {
        SearchPattern pattern = SearchPattern.createPattern(toSearchFor, IJavaSearchConstants.REFERENCES);
        IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] {javaProject});
        MockSearchRequestor requestor = new MockSearchRequestor();
        new SearchEngine().search(pattern, new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()}, scope, requestor, null);
        return requestor;
    }

    private void assertMatches(String toFind, MockSearchRequestor requestor, int allMatches, int firstMatches) {
//        for (Iterator<SearchMatch> it = requestor.matches.iterator(); it.hasNext();) {
//            IJavaElement elem = (IJavaElement) it.next().getElement();
//            IJavaElement type = elem.getAncestor(IJavaElement.TYPE);
//            if (type != null && type.getElementName().equals("XMLDTDScannerImpl")) {
//                it.remove();
//            }
//        }
        if (requestor.matches.size() != allMatches) {
            fail("Expecting " + allMatches + " matches, but found " + requestor.matches.size() + "\n" + requestor.printMatches());
        }
        int currIndex = groovyClassContents.indexOf("def doit") + "def doit".length();
        for (int i = 0; i < firstMatches; i += 1) {
            SearchMatch match = requestor.matches.get(i);
            currIndex = groovyClassContents.indexOf(toFind, currIndex);
            assertEquals("Invalid start position in match " + i + "\n" + requestor.printMatches(), currIndex, match.getOffset());
            assertEquals("Invalid length in match " + i + "\n" + requestor.printMatches(), toFind.length(), match.getLength());
            currIndex += toFind.length();
        }
        currIndex = groovyClassContents2.indexOf("def doit") + "def doit".length();
        for (int i = firstMatches; i < allMatches; i += 1) {
            SearchMatch match = requestor.matches.get(i);
            currIndex = groovyClassContents2.indexOf(toFind, currIndex);
            assertEquals("Invalid start position in match " + i + "\n" + requestor.printMatches(), currIndex, match.getOffset());
            assertEquals("Invalid length in match " + i + "\n" + requestor.printMatches(), toFind.length(), match.getLength());
            currIndex += toFind.length();
        }
    }

    //

    public void testClassDecl1() throws Exception {
        IType type = javaProject.findType("pack.AGroovyClass");
        MockSearchRequestor requestor = performSearch(type);
        assertMatches("AGroovyClass", requestor, 12, 2);
    }

    public void testClassDecl2() throws Exception {
        IType type = javaProject.findType("pack.OtherClass");
        MockSearchRequestor requestor = performSearch(type);
        assertMatches("OtherClass", requestor, 4, 2);
    }

    public void testFieldDecl1() throws Exception {
        IType type = javaProject.findType("pack.AGroovyClass");
        String toFind = "age_1";
        IField field = type.getField(toFind);
        MockSearchRequestor requestor = performSearch(field);
        assertMatches(toFind, requestor, 4, 2);
    }

    public void testFieldDecl2() throws Exception {
        IType type = javaProject.findType("pack.AGroovyClass");
        String toFind = "name_1";
        IField field = type.getField(toFind);
        MockSearchRequestor requestor = performSearch(field);
        assertMatches(toFind, requestor, 4, 2);
    }

    public void testMethodDecl() throws Exception {
        IType type = javaProject.findType("pack.AGroovyClass");
        String toFind = "doit";
        IMethod method = type.getMethod(toFind, new String[0]);
        MockSearchRequestor requestor = performSearch(method);
        assertMatches(toFind, requestor, 4, 2);
    }

    public void testFieldRefInInitializer() throws Exception {
        IType type = javaProject.findType("pack.AGroovyClass");
        String toFind = "fieldInInitializer";
        IField method = type.getField(toFind);
        MockSearchRequestor requestor = performSearch(method);
        assertMatches(toFind, requestor, 2, 1);
    }

    public void testMethodRefInInitializer() throws Exception {
        IType type = javaProject.findType("pack.AGroovyClass");
        String toFind = "referencedInInitializer";
        IMethod method = type.getMethod(toFind, new String[0]);
        MockSearchRequestor requestor = performSearch(method);
        assertMatches(toFind, requestor, 2, 1);
    }
}
