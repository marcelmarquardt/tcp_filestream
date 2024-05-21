#ifndef CLIENT_H
#define CLIENT_H

#include <memory>
#include <optional>
#include <string>
#include <boost/asio/streambuf.hpp>
#include <boost/asio/ip/tcp.hpp>

class Client
{
public:
    static Client* get(const char* host_name, int port);

    [[nodiscard]] std::optional<std::pair<std::unique_ptr<std::byte[]>, size_t>> receive_file() const;

private:
    Client(const std::string& host_name, int port);
    ~Client();

    bool write(const char* buffer, size_t buffer_size) const;
    [[nodiscard]] std::unique_ptr<boost::asio::streambuf> read() const;
    void print_progress(int i, int max) const;

    std::unique_ptr<boost::asio::ip::tcp::socket> _socket;
};

#endif //CLIENT_H
