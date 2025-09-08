#pragma once

#include <string>
#include <vector>

namespace controller {
class RemoteController {
public:
    virtual bool isConnected() = 0;
    virtual void send(std::string const&) = 0;
    virtual void advertise() = 0;
    virtual void disconnect() = 0;
    virtual std::vector<std::string> getMessages() = 0;
};

}