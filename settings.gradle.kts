rootProject.name = "KingdomTreasury"
include(":BMLBuilder", ":QuestionLibrary", ":WurmTestingHelper")
project(":BMLBuilder").projectDir = file("../BMLBuilder")
project(":QuestionLibrary").projectDir = file("../QuestionLibrary")
project(":WurmTestingHelper").projectDir = file("../WurmTestingHelper")

