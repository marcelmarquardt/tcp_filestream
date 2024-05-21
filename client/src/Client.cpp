#include "Client.h"

#include <iostream>
#include <thread>
#include <boost/array.hpp>
#include <boost/asio/completion_condition.hpp>
#include <boost/asio/read.hpp>
#include <boost/asio/write.hpp>

#include "model.h"

#define CHUNK_SIZE 8192

Client* Client::get(const char* host_name, int port)
{
    static Client client(host_name, port);
    return &client;
}

void Client::print_progress(int i, int max)
{
    i--; // not 0 based
    if (i == 0)
    {
        std::cout << "PROGRESS [" << std::flush;
    }

    // 11 steps total [==========]
    static int total_steps = 10;
    int step = max / total_steps;
    if ((step - i) % step == 0)
        std::cout << "=" << std::flush;

    if (i >= max)
    {
        std::cout << "]\n" << std::flush;
    }

    // used for debugging, file stream too fast
    // std::this_thread::sleep_for(std::chrono::milliseconds(2));
}

std::optional<std::pair<std::unique_ptr<std::byte[]>, size_t>> Client::receive_file() const
{
    model::Message msg;
    if (!write(reinterpret_cast<char*>(&msg), sizeof(msg)))
    {
        std::cerr << "Failed to ask for file size\n";
        return std::nullopt;
    }

    size_t file_size = std::atoi(boost::asio::buffer_cast<const char*>(read()->data()));
    if (file_size <= 0)
    {
        std::cerr << "Failed to read file size\n";
        return std::nullopt;
    }

    std::cout << "Received file size: " << file_size << " bytes\nStarting filestream\n";

    // allocate one additional chunk for safety
    auto file = std::make_unique<std::byte[]>(file_size + CHUNK_SIZE);
    uintptr_t buffer_start = reinterpret_cast<uintptr_t>(file.get());
    uintptr_t buffer_end = buffer_start + file_size;

    int i = 0;
    int i_max = (buffer_end - buffer_start) / CHUNK_SIZE;
    while (buffer_start < buffer_end)
    {
        model::Message file_chunk(i++);

        if (!write(reinterpret_cast<char*>(&file_chunk), sizeof(file_chunk)))
        {
            std::cerr << "Failed to ask for file chunk\n";
            return std::nullopt;
        }

        boost::array<char, CHUNK_SIZE> buf = {};
        size_t bytes_transferred = boost::asio::read(*_socket, boost::asio::buffer(buf),
            boost::asio::transfer_at_least(1));
        memcpy(reinterpret_cast<void*>(buffer_start), buf.data(), bytes_transferred);
        buffer_start += bytes_transferred;
        print_progress(i, i_max);
    }

    return std::make_pair(std::move(file), file_size);
}

std::unique_ptr<boost::asio::streambuf> Client::read() const
{
    auto read_buffer = std::make_unique<boost::asio::streambuf>();
    boost::asio::read(*_socket, *read_buffer,
        boost::asio::transfer_at_least(1));
    return read_buffer;
}

bool Client::write(const char* buffer, size_t buffer_size) const
{
    boost::system::error_code error;
    auto bytes_written = boost::asio::write(*_socket,
        boost::asio::buffer(buffer, buffer_size), error);
    return !(error || buffer_size != bytes_written);
}

Client::Client(const std::string& host_name, int port)
{
    std::cout << "Connecting to TCP Server: " << host_name << ":" << port << "\n";

    using namespace boost::asio::ip;
    try
    {
        boost::asio::io_context io_context;
        tcp::resolver resolver(io_context);
        tcp::resolver::query query(host_name, std::to_string(port));

        _socket = std::make_unique<tcp::socket>(io_context);

        boost::system::error_code error;
        _socket->lowest_layer().connect(*resolver.resolve(query), error);
        if (error)
            std::cerr << "Connect failed: " << error << "\n";

        std::cout << "Connected successfully\n";
    }
    catch (std::exception& e)
    {
        std::cerr << "Exception: " << e.what() << "\n";
    }
}

Client::~Client()
{
    _socket->lowest_layer().close();
}