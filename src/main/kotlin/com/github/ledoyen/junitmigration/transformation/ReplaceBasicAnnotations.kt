package com.github.ledoyen.junitmigration.transformation

import com.github.ledoyen.minimaldiffparser.MinimalDiffCompilationUnit
import com.github.ledoyen.minimaldiffparser.MinimalDiffVisitorAdapter

class ReplaceBasicAnnotations : MinimalDiffVisitorAdapter() {

    override fun visit(cu: MinimalDiffCompilationUnit) {
        cu.replaceTextByRegex("import\\s+org\\.junit\\.Test;", "import org.junit.jupiter.api.Test;")

        cu.replaceTextByRegex("import\\s+org\\.junit\\.\\*;", "import org.junit.jupiter.api.*;")
        cu.replaceTextByRegex("import\\s+org\\.junit\\.BeforeClass;", "import org.junit.jupiter.api.BeforeAll;")
        cu.replaceTextByRegex("@BeforeClass(\\s)") { m -> "@BeforeAll" + m.group(1) }
        cu.replaceTextByRegex("import\\s+org\\.junit\\.AfterClass;", "import org.junit.jupiter.api.AfterAll;")
        cu.replaceTextByRegex("@AfterClass(\\s)") { m -> "@AfterAll" + m.group(1) }

        cu.replaceTextByRegex("import\\s+org\\.junit\\.Before;", "import org.junit.jupiter.api.BeforeEach;")
        cu.replaceTextByRegex("@Before(\\s)") { m -> "@BeforeEach" + m.group(1) }
        cu.replaceTextByRegex("import\\s+org\\.junit\\.After;", "import org.junit.jupiter.api.AfterEach;")
        cu.replaceTextByRegex("@After(\\s)") { m -> "@AfterEach" + m.group(1) }
    }
}