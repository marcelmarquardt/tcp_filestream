#include <iostream>

#include <fstream>
#include <optional>

#include "Client.h"

#define HOST_NAME "localhost"
#define PORT 8080

int main()
{
    auto file = Client::get(HOST_NAME, PORT)->receive_file();
    if (!file.has_value())
    {
        std::cerr << "Failed to receive file";
        return 1;
    }

    std::cout << "Successfully received file\nDumping file to disk (dump.jpg)\n";
    std::ofstream ofs("dump.jpg");
    ofs.write(reinterpret_cast<char*>(file->first.get()), file->second);
    ofs.close();
    std::cout << "File dumped successfully";

    return 0;
}
