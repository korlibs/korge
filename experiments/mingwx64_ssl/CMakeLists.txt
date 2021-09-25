cmake_minimum_required(VERSION 3.20)
project(untitled2)

set(CMAKE_CXX_STANDARD 14)

add_executable(untitled2 main.cpp)

target_link_libraries(untitled2 ws2_32.lib Crypt32.lib Secur32.lib Comctl32.lib)
