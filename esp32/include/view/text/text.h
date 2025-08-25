#pragma once

#include "view/view.h"

namespace view{
    class Text: public View{
        public:

        Text(RectType frame, View* superiorView, std::function<void(ble::UpdateMessage const&, std::string&)> onEvent, std::string const& content): View::View(frame, superiorView, "text"), m_content(content), m_onEvent(onEvent){
            
        }

        void draw() override {
            //TODO
        }

        void onEvent(ble::UpdateMessage const& event) override {
            m_onEvent(event, m_content);
        }

        private:

        std::string m_content;

        std::function<void(ble::UpdateMessage const&, std::string&)> m_onEvent;
    };
}

