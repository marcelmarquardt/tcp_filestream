cmake_minimum_required(VERSION 3.28)
project(client)

set(CMAKE_CXX_STANDARD 17)

set(Boost_USE_STATIC_LIBS OFF)
set(Boost_USE_MULTITHREADED ON)
set(Boost_USE_STATIC_RUNTIME OFF)
find_package(Boost COMPONENTS
        system
        filesystem)
message(STATUS "Boost version: ${Boost_VERSION}")

add_executable(client src/main.cpp
        src/model.h
        src/model.h
        src/Client.cpp
        src/Client.h
        src/Client.h)

target_link_libraries(client
        Boost::system
)