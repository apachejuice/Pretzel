rootProject.name = "Pretzel"
include("src:test")
findProject(":src:test")?.name = "test"
