import os
Import("env")
# include toolchain paths
env.Replace(COMPILATIONDB_INCLUDE_TOOLCHAIN=True)
# override compilation DB path
file_destination = os.path.join(
    env["PROJECT_DIR"], "compile_commands.json")
print(f'file destination for compile_commands.json: {file_destination}')
env.Replace(COMPILATIONDB_PATH=file_destination)
