#ifndef MODEL_H
#define MODEL_H

namespace model
{
    struct Message
    {
        enum EMessage
        {
            FILE_SIZE,
            FILE_CHUNK
        };

        Message() : id(FILE_SIZE)
        {}
        explicit Message(int file_chunk) : id(FILE_CHUNK), file_chunk(file_chunk)
        {}

        const EMessage id;
        const int file_chunk{};
    };
}

#endif //MODEL_H
